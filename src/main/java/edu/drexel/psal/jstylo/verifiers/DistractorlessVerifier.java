package edu.drexel.psal.jstylo.verifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import edu.drexel.psal.jstylo.machineLearning.Verifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Heavily modified version of Ariel Stolerman's original DistractorlessCV class produced for Classify-Verify.
 * There are two large differences:
 * 		1) The feature extraction occurs elsewhere; currently through JStyki API calls. The extracted features are passed in as Instances objects
 * 		2) This version has a simplified threshold determination method.
 * 			>The original performed leave one out cross validation in order to determine the ideal threshold
 * 			>This version takes the average diff and then applies a modifier.
 * 			>Plans are in place to build the original's method into either this verifier or a subclass 
 * @author Travis Dutko
 */
public class DistractorlessVerifier extends Verifier{

    private static final long serialVersionUID = 1L;
    private String trainAuthor; //the author whose training data we are using to perform verification
	private String analysisString; //string of data to be analyzed
	private List<DistractorlessEvaluation> evaluations; //stores each experiment
	private boolean metaVerification; //whether or not we actually know the authors of the documents.
	private double[] centroid;
	private double TPThresholdRate;
	
	private Instances trainingInstances; //known documents
	private Instances testingInstances; //documents to be verified
	
	private double thresholdModifier; //the modifier to be applied to the threshold
	
	/**
	 * Basic constructor. Uses the default threshold modifier of 0.
	 * This should equate to roughly 50% of the known documents being classified correctly (as we're just taking the mean diff).
	 * @param tri
	 * @param tei
	 */
	public DistractorlessVerifier(Instances tri, Instances tei, boolean meta){
		//grab the instances
		trainingInstances = tri;
		testingInstances = tei;
		//and set class indicies
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		if (testingInstances != null){
			testingInstances.setClassIndex(testingInstances.numAttributes()-1);
		}
		trainAuthor = trainingInstances.instance(0).attribute(trainingInstances.instance(0).classIndex()).value((int) trainingInstances.instance(0).classValue());
		System.out.println("TrainAuthor: "+trainAuthor);
		//initialize data structures
		analysisString = "";
		evaluations = new ArrayList<DistractorlessEvaluation>();
		
		metaVerification = meta;
	}
	
	/**
	 * Constructor for use with a desired true positive rate on known data. this will pick the narrowest threshold that meets this criteria for analysis.
	 * @param tri
	 * @param tei
	 * @param meta
	 * @param rate
	 */
	public DistractorlessVerifier(Instances tri, Instances tei, boolean meta, double rate){
		//grab the instances
		trainingInstances = tri;
		testingInstances = tei;
		//and set class indicies
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		if (testingInstances != null){
			testingInstances.setClassIndex(testingInstances.numAttributes()-1);
		}
		trainAuthor = trainingInstances.instance(0).attribute(trainingInstances.instance(0).classIndex()).value((int) trainingInstances.instance(0).classValue());
		//initialize data structures
		analysisString = "";
		evaluations = new ArrayList<DistractorlessEvaluation>();
		
		metaVerification = meta;

		//set rate
		TPThresholdRate = rate;
		thresholdModifier = Double.MIN_VALUE;
	}
	
	/*
	 * Constructor that also takes a threshold modifier.
	 * NOT CURRENTLY USED AS IT IS TYPICALLY INEFFECTIVE.
	 * Plans are in place for developing an automated way to determine the ideal threshold, but for now put the power in the user's hands.
	 * To big of a positive modifier (ie 1.00) will likely result in a lot of false positives, whereas too low (-1.00) would have a lot of false negatives.
	 * @param tri
	 * @param tei
	 * @param modifier
	 */
/*	public DistractorlessVerifier(Instances tri, Instances tei, double modifier, boolean meta) {
		
		//grab the instances
		trainingInstances = tri;
		testingInstances = tei;
		//and set class indicies
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		if (testingInstances != null){
			testingInstances.setClassIndex(testingInstances.numAttributes()-1);
		}
		trainAuthor = trainingInstances.instance(0).attribute(trainingInstances.instance(0).classIndex()).value((int) trainingInstances.instance(0).classValue());
		//initialize data structures
		analysisString = "";
		evaluations = new ArrayList<DistractorlessEvaluation>();
		
		metaVerification = meta;
		
		//grab threshold modifier
		thresholdModifier = modifier;
		TPThresholdRate = Double.MIN_VALUE;
	} */
	
	public void setTestInstances(Instances tei){
		testingInstances = tei;
	}
	
	/**
	 * The actual verification method
	 */
	@Override
	public void verify() {

		calculateCentroid();
		
		analysisString+=String.format("problem,test_author,train_author,dist\n");
		
		// Calculates the average distance between all documents
		analysisString = runAnalysis();
		
		Double thresh = 0.0;
		// Create a list of evaluations--one for each testing instance
		if (TPThresholdRate == Double.MIN_VALUE){
			//System.out.println("Generating threshold based on average times a multiplier");
			thresh = calculateDesiredThreshold(0.0); // grab the threshold from the analysis string
			thresh = thresh + thresh * thresholdModifier; // apply the modifier
		} else {
			//System.out.println("Generating threshold bassed on desired true positive rate on known data");
			thresh = calculateDesiredThreshold(TPThresholdRate);
		}
		
		//System.out.println("Thresh: "+thresh);
		
		// for all testing documents
		for (int i = 0; i < testingInstances.numInstances(); i++) {
			
			Instance inst = testingInstances.instance(i); // grab the instance
			DistractorlessEvaluation de = new DistractorlessEvaluation(inst.attribute(inst.classIndex()).value((int) inst.classValue()),inst);
			double distance = calculateDistanceFromCentroid(inst.toDoubleArray());
			de.setDistance(distance);
			// then build the string to interpret from the results
			String docString = trainingInstances.instance(0).attribute(trainingInstances.instance(0).classIndex())
					.value((int) trainingInstances.instance(0).classValue())
					+ "," + inst.attribute(inst.classIndex()).value((int) inst.classValue()) + "," + (distance);
			docString+=",testDoc";
			//System.out.println("Distance for test doc "+i+" = "+total/count);
			try {
				// and create the evaluation with it via evalCSV
				de.setResultEval(Distractorless.evalCSV(analysisString + docString, thresh, metaVerification));
				evaluations.add(de);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * returns the string of all of the docs with their diff values
	 * @return
	 */
	public String getAnalysisString(){
		return analysisString;
	}
	
	/**
	 * Returns the statistics strings created by the weka evaluation objects
	 */
	@Override
	public String getResultString() {
		String s = "";
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;
		for (DistractorlessEvaluation de : evaluations){
			s+=de.getResultString();
			if(metaVerification){
				int result = de.getCorrectlyVerified(trainAuthor);
				s+="Result is a ";
				if (result == 0){
					s+="true positive\n\n";
					tp++;
				} else if (result == 1){
					s+="false positive\n\n";
					fp++;
				} else if (result == 2){
					s+="false negative\n\n";
					fn++;
				} else if (result == 3){
					System.out.println();
					s+="true negative\n\n";
					tn++;
				} else {
					s+="undefined\n\n";
				}
			}
		}
		
		
		if (metaVerification){
			s+=String.format("Overall Result Counts: \nTrue Positives: %d\nFalse Positives: %d\nFalse Negatives: %d\nTrue Negatives: %d\nTotal experiments: %d",
					tp,fp,fn,tn,(tp+fp+fn+tn));
		}
		
		return s;
	}
	
	public String getCleanResultString(){
		String s = "";
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;
		for (DistractorlessEvaluation de : evaluations){
			if(metaVerification){
				int result = de.getCorrectlyVerified(trainAuthor);
				if (result == 0){
					tp++;
				} else if (result == 1){
					fp++;
				} else if (result == 2){
					fn++;
				} else if (result == 3){
					tn++;
				} else {
					;
				}
			}
		}
		
		if (metaVerification){
			s+=String.format("Overall Result Counts: \nTrue Positives: %d\nFalse Positives: %d\nFalse Negatives: %d\nTrue Negatives: %d\nTotal experiments: %d",
					tp,fp,fn,tn,(tp+fp+fn+tn));
		}
		
		return s;
	}
	
	public int[] getRates(){
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;
		for (DistractorlessEvaluation de : evaluations){
			if(metaVerification){
				int result = de.getCorrectlyVerified(trainAuthor);
				if (result == 0){
					tp++;
				} else if (result == 1){
					fp++;
				} else if (result == 2){
					fn++;
				} else if (result == 3){
					tn++;
				}
			}
		}
		int[] results = new int[4];
		results[0]=tp;
		results[1]=fp;
		results[2]=fn;
		results[3]=tn;
		return results;
	}
	
	/*
	 * 
	 */
	@Override
	public double getAccuracy(){
		double acc = 0.0;
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tn = 0;
		for (DistractorlessEvaluation de : evaluations){
			if(metaVerification){
				int result = de.getCorrectlyVerified(trainAuthor);
				if (result == 0){
					tp++;
				} else if (result == 1){
					fp++;
				} else if (result == 2){
					fn++;
				} else if (result == 3){
					tn++;
				}
			}
		}
		acc = (((double)(tp + tn)) / ((double)(tp+tn+fp+fn)));  
		return acc;
	}
	
	/**
	 * Calculates the average distance between the author's documents and returns that as the threshold baseline
	 * @return
	 */
	private double calculateDesiredThreshold(double rate){
		
		String aCopy = new String(analysisString);
		double cumulative = 0.0;
		double max = 0.0;
		int count = 0;
		String[] line;
		Scanner s = new Scanner(aCopy);
		s.nextLine(); //skip the header
		List<Double> thresholds = new ArrayList<Double>();
		//collect all of the numbers
		while (s.hasNext()){
			String string = s.nextLine();
			line = string.split(",");
			double value = Double.parseDouble(line[line.length-1]);
			/*if (value == Double.NaN){
				System.out.println("NaN from: "+line);
			} else {
				System.out.println("In-process assessment: " + value);
				System.out.println("Line: "+string);
			}*/
			if (value > max){
				max = value;
			}
			thresholds.add(value);
			cumulative += value;
			count++;
		}
		
		s.close();
		
		//if we're using the average * multiplier method, just return the average
		if (rate == 0.0){
			return (cumulative/count);
		}else if (rate == 1.0){
			Collections.sort(thresholds);
			return thresholds.get(thresholds.size()-1);
		//otherwise, it's time to do some extra math
		} else {
			int goal = -1;
			for (int i = 0; i<count; i++){
				if ((i * (1.0/count)) > rate){
					goal = i;
					break;
				}
			}
			
			//if we can't find something satisfactory for some reason, return the average
			if (goal == -1){
				return (cumulative/count);
			} else {
				Collections.sort(thresholds);
				/*
				System.out.println("To get at least a rate of: "+rate+" we need a threshold of: "+thresholds.get(goal));
				System.out.println("For comparison, the average distance is: "+cumulative/count);
				*/
				return thresholds.get(goal);
			}
		}
	}

	private void calculateCentroid(){

		centroid = new double[trainingInstances.numAttributes()-2];
		
		for (int i = 0; i < centroid.length; i++){
			centroid[i] = 0;
		}
		
		for (int j = 0; j < trainingInstances.numInstances(); j++){
			for (int i = 1; i < trainingInstances.numAttributes()-1; i++){
				centroid[i-1] += trainingInstances.instance(j).value(i);
			}
		}
		
		for (int i = 0; i < centroid.length; i++){
			centroid[i] = centroid[i]/trainingInstances.numInstances();
		}
		
		/*
		int nominalFeatures = 0;
		
		for (int j = 0; j < trainingInstances.numAttributes(); j++){
			if (trainingInstances.attribute(j).isNominal()){
				nominalFeatures++;
			}
		}
		
		centroid  = new double[trainingInstances.numAttributes()-nominalFeatures];
		int count = trainingInstances.numInstances();
		
		for (int i = 0; i < centroid.length; i++){
			centroid[i]=0;
		}
		
		for (int i = 0; i < count; i++){
			int centCount = 0;
			Instance inst = trainingInstances.instance(i);
			for (int j = 0; j < trainingInstances.numAttributes(); j++){
				if (!trainingInstances.attribute(j).isNominal()){
					centroid[centCount] = inst.value(j);
					centCount++;
				}
			}
		}
		
		for (int i = 0; i < centroid.length; i++){
			centroid[i]= centroid[i]/trainingInstances.numInstances();
		}
		*/
	}
	
	private String runAnalysis(){
		String s = "";
		for (int j = 0; j < trainingInstances.numInstances(); j++) {
			Instance instJ = trainingInstances.instance(j);
			Double dist = calculateDistanceFromCentroid(instJ.toDoubleArray());
			// format is train author, test author, cosine distance
			// DO NOT CHANGE
			// unless you also change the Distractorless.java's evalCSV method to compensate
			s += String.format(instJ.attribute(instJ.classIndex()).value((int) instJ.classValue()) + ","
					+ instJ.attribute(instJ.classIndex()).value((int) instJ.classValue()) + ","
					+ dist + "\n");
		}
		return s;
	}
	
	private double calculateDistanceFromCentroid(double[] b){
		Double dot = 0.0; //cumulative dot value
		Double normA = 0.0; //cumulative norm value for doc a
		Double normB = 0.0; //cumulative norm value for doc b
		int n = b.length; //number of features
		double[] a = centroid;
		
		int centroidIndex = 0;
		//for all features
		for (int i = 1; i < n-1; i++){
			//if it isn't the class feature
			
			if (trainingInstances.instance(0).attribute(i).type() == Attribute.NOMINAL){
				continue;
			} else {
				//FIXME later figure out why these conditions sometimes prove true.
				if (!(Double.isNaN(b[i]) || Double.isNaN(a[centroidIndex]))) {
					// calculate the dot and norm values
					dot += a[centroidIndex] * b[i];
					normA += Math.pow(a[centroidIndex], 2);
					normB += Math.pow(b[i], 2);
					centroidIndex++;
				} else {
					/*if (Double.isNaN(b[i])){
						System.out.println("b[i] is NaN at i: "+i);
					}
					if (Double.isNaN(a[centroidIndex])){
						System.out.println("a[centroidIndex] is NaN at: "+centroidIndex);
					}*/
					centroidIndex++;
				}
			}
		}
		//simple math to calculate the final value
		double val = 1-(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
		return val;
	}
	
	/**
	 * Calculates the similarity of two feature vectors
	 * @param a
	 * @param b
	 * @return
	 */
	@SuppressWarnings("unused")
    private double calculateCosineDistance(double[] a, double[] b){
		Double dot = 0.0; //cumulative dot value
		Double normA = 0.0; //cumulative norm value for doc a
		Double normB = 0.0; //cumulative norm value for doc b
		int n = a.length; //number of features
		if (a.length != b.length){
			//System.out.println("Vectors are of different length! This is going to get messy...");
			//System.out.println("a length: "+a.length+" b length: "+b.length);
			//honestly, it might be better to just break it if this is wrong.
			//this means that either one has more features or something of that ilk
			if (b.length < a.length)
				n = b.length; //but for now just compare the number of features both docs do have and hope for the best
		}
		
		//for all features
		for (int i = 1; i < n-1; i++){
			//if it isn't the class feature
			
			if (trainingInstances.instance(0).attribute(i).type() == Attribute.NOMINAL){
				continue;
			} else {
				//calculate the dot and norm values
				dot += a[i]*b[i];
				normA += Math.pow(a[i],2);
				normB += Math.pow(b[i],2);
			}
		}
		
		//simple math to calculate the final value
		double val = 1-(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
		return val;
	}
	
	public List<DistractorlessEvaluation> getInternalEvaluations(){
		
		return evaluations;
	}
	
	public List<Evaluation> getResultEvaluations(){
		List<Evaluation> results = new ArrayList<Evaluation>();
		for (DistractorlessEvaluation de : evaluations){
			results.add(de.getResultEval());
		}
		return results;
	}
	
	public Evaluation getResultsEval(){
		return evaluations.get(0).getResultEval();
	}
}

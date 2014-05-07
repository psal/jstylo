package edu.drexel.psal.jstylo.verifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import edu.drexel.psal.jstylo.generics.Verifier;

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

	private String analysisString; //string of data to be analyzed
	private List<Evaluation> documentEvaluations; //stores the weka Evaluation objects for the verification
	
	private Instances trainingInstances; //known documents
	private Instances testingInstances; //documents to be verified
	
	private double thresholdModifier; //the modifier to be applied to the threshold
	
	/**
	 * Basic constructor. Uses the default threshold modifier of 0.
	 * This should equate to roughly 50% of the known documents being classified correctly (as we're just taking the mean diff).
	 * @param tri
	 * @param tei
	 */
	public DistractorlessVerifier(Instances tri, Instances tei){
		this(tri,tei,0.0);
	}
	
	/**
	 * Constructor that also takes a threshold modifier.
	 * Plans are in place for developing an automated way to determine the ideal threshold, but for now put the power in the user's hands.
	 * To big of a positive modifier (ie 1.00) will likely result in a lot of false positives, whereas too low (-1.00) would have a lot of false negatives.
	 * @param tri
	 * @param tei
	 * @param modifier
	 */
	public DistractorlessVerifier(Instances tri, Instances tei, double modifier) {
		
		//grab the instances
		trainingInstances = tri;
		testingInstances = tei;
		//and set class indicies
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		if (testingInstances != null){
			testingInstances.setClassIndex(testingInstances.numAttributes()-1);
		}
		
		//initialize data structures
		analysisString = "";
		documentEvaluations = new ArrayList<Evaluation>();
		
		//grab threshold modifier
		thresholdModifier = modifier;
	}

	/**
	 * The actual verification method
	 */
	@Override
	public void verify() {

		analysisString+=String.format("problem,test_author,train_author,dist\n");
		//TODO Perform this "leave one out" for a more sophisticated way of establishing the desired threshold?
		//List<Document> documents, docs;
		//Document doc;
		
		// =====================================================================
		// author leave-one-out on own, known documents: This is used to establish the threshold
		// =====================================================================
		/*for (String author: ps.getAuthors())
		{
			docs = ps.getTrainDocs(author);
			// iterate over author's docs and each time take one to be the
			// test doc
			for (int i = 0; i < docs.size(); i++)
			{
				// prepare documents
				documents = new LinkedList<>();
				for (int j = 0; j < docs.size(); j++)
				{
					doc = docs.get(j);
					// test doc
					if (i == j)
						documents.add(new Document(
								doc.getFilePath(),
								null,
								"Correct: " + doc.getAuthor()));
					else
						documents.add(new Document(
								doc.getFilePath(),
								doc.getAuthor(),
								doc.getTitle()));
				}
				
				//run the leave one out classification
				
			}
		}*/
		
		// Calculates the average distance between all documents
		analysisString = runAnalysis();

		// Create a list of evaluations--one for each testing instance
		Double thresh = calculateDesiredThreshold(); // grab the threshold from the analysis string
		thresh = thresh + thresh * thresholdModifier; // apply the modifier

		// for all testing documents
		for (int i = 0; i < testingInstances.numInstances(); i++) {
			Instance inst = testingInstances.get(i); // grab the instance
			double total = 0.0;
			int count = 0;
			// calculate the CosineDistance between the document and each of the author's documents
			for (int j = 0; j < trainingInstances.numInstances(); j++) {
				total += calculateCosineDistance(trainingInstances.get(j).toDoubleArray(), inst.toDoubleArray());
				count++;
			}
			// then build the string to interpret from the results
			String docString = trainingInstances.get(0).attribute(trainingInstances.get(0).classIndex())
					.value((int) trainingInstances.get(0).classValue())
					+ "," + inst.attribute(inst.classIndex()).value((int) inst.classValue()) + "," + (total / count);
			try {
				// and create the evaluation with it via evalCSV
				documentEvaluations.add(Distractorless.evalCSV(analysisString + docString, thresh));
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
		int count = 0;
		for (Evaluation e : documentEvaluations) {
			Instance inst = testingInstances.get(count);
			try {
				s += "<=========["+inst.stringValue(inst.attribute(0))+"]=========>";
				s += /*e.toSummaryString() + "\n" + e.toClassDetailsString() + "\n" +*/ e.toMatrixString() + "\n";
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			count++;
		}
		return s;
	}
	
	/**
	 * Calculates the average distance between the author's documents and returns that as the threshold baseline
	 * @return
	 */
	private double calculateDesiredThreshold(){
		String aCopy = new String(analysisString);
		double cumulative = 0.0;
		int count = 0;
		String[] line;
		Scanner s = new Scanner(aCopy);
		s.nextLine(); //skip the header
		//collect all of the numbers
		while (s.hasNext()){
			line = s.nextLine().split(",");
			cumulative += Double.parseDouble(line[line.length-1]);
			count++;
		}
		
		s.close();
		
		//divide cumulative by the total number of docs
		return (cumulative/count);
	}

	private String runAnalysis(){
		String s = "";
		//for all pairs of instances
		for (int i = 0; i < trainingInstances.numInstances(); i++){
			Instance instI = trainingInstances.get(i);
			for (int j = 0; j<trainingInstances.numInstances(); j++){
				Instance instJ = trainingInstances.get(j);
				
				//if they are not the same instance
				if (instI != instJ){
					//if they are by the same author
					if(instI.classValue() == instJ.classValue()){
						//format is train author, test author, cosine distance
						//DO NOT CHANGE
						//unless you also change the Distractorless.java's evalCSV method to compensate
						s+=String.format(
								instI.attribute(instI.classIndex()).value((int)instI.classValue())+","+
								instJ.attribute(instJ.classIndex()).value((int)instJ.classValue())+","+
								calculateCosineDistance(instI.toDoubleArray(),instJ.toDoubleArray())+"\n"
								);
					}
				}
			}
		}
		return s;
	}
	
	/**
	 * Calculates the similarity of two feature vectors
	 * @param a
	 * @param b
	 * @return
	 */
	private double calculateCosineDistance(double[] a, double[] b){
		Double dot = 0.0; //cumulative dot value
		Double normA = 0.0; //cumulative norm value for doc a
		Double normB = 0.0; //cumulative norm value for doc b
		int n = a.length; //number of features
		if (a.length != b.length){
			System.out.println("Vectors are of different length! This is going to get messy...");
			//honestly, it might be better to just break it if this is wrong.
			//this means that either one has more features or something of that ilk
			if (b.length < a.length)
				n = b.length; //but for now just compare the number of features both docs do have and hope for the best
		}
		
		//for all features
		for (int i = 0; i < n; i++){
			//if it isn't the class feature
			if (trainingInstances.get(0).attribute(i).type() == Attribute.NOMINAL){
				continue;
			} else {
				if (a[i] != 0 && b[i] != 0){
				//calculate the dot and norm values
				dot += a[i]*b[i];
				normA += Math.pow(a[i],2);
				normB += Math.pow(b[i],2);
				}
			}
		}
		
		//simple math to calculate the final value
		return 1-(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
	}
	
}

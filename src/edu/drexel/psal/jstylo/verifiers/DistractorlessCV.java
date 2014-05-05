package edu.drexel.psal.jstylo.verifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.jgaap.backend.API;
import com.jgaap.canonicizers.NormalizeWhitespace;
import com.jgaap.canonicizers.UnifyCase;
import com.jgaap.distances.HistogramDistance;
import com.jgaap.eventDrivers.CharacterNGramEventDriver;
import com.jgaap.eventDrivers.WordNGramEventDriver;
import com.jgaap.generics.AnalysisDriver;
import com.jgaap.generics.Canonicizer;
import com.jgaap.generics.Document;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.Pair;

import utils.CentroidDriverSD;
import weka.classifiers.Evaluation;
import weka.core.Instance;
import weka.core.Instances;
import edu.drexel.psal.jstylo.generics.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.generics.FeatureDriver;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import edu.drexel.psal.jstylo.generics.Verifier;

public class DistractorlessCV extends Verifier{

	private String analysisString; //string of data to be analyzed
	private String resultsString; //final results to be returned
	private ProblemSet ps;
	private Instances trainingInstances;
	private Instances testingInstances;
	private double thresholdModifier;
	
	public DistractorlessCV(ProblemSet p, Instances tri, Instances tei, double modifier) {
		ps=p;
		trainingInstances = tri;
		testingInstances = tei;
		trainingInstances.setClassIndex(trainingInstances.numAttributes()-1);
		if (testingInstances != null){
			testingInstances.setClassIndex(testingInstances.numAttributes()-1);
		}
		resultsString = "";
		analysisString = "";
		thresholdModifier = modifier;
	}

	@Override
	public void verify() {

		analysisString+=String.format("problem,test_author,train_author,dist\n");
		List<Document> documents, docs;
		Document doc;
		
		// =====================================================================
		// author leave-one-out on own, known documents: This is used to establish the threshold
		// =====================================================================
		for (String author: ps.getAuthors())
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
				runAnalysis();
			}
		}
		
		//by this point, we now have the distance measures between the author and all of his/her documents
		/*
		 * TODO use the runAnalysis / leave one out results to pick the desired threshold value
		 * To accomplish this, allow the Verifier to have a target TP or FP rate, and then aim to hit that rate while max/minning other stats
		 * provide a method which parses the "resultsString" picks the appropriate threshold, then runs a single verification on the unknown document.
		 */
		List<Evaluation> documentEvaluations = new ArrayList<Evaluation>();
		Double thresh = calculateDesiredThreshold();
		thresh = thresh + thresh * thresholdModifier;
		for (int i = 0; i < testingInstances.numInstances(); i++){
			Instance inst = testingInstances.get(i);
			double total = 0.0;
			int count = 0;
			for (int j = 0; j < trainingInstances.numInstances(); j++){
				total += calculateCosineDistance(trainingInstances.get(j).toDoubleArray(),inst.toDoubleArray());
				count ++;
			}
			String docString = trainingInstances.get(0).attribute(trainingInstances.get(0).classIndex()).value((int)trainingInstances.get(0).classValue())+","+
					inst.attribute(inst.classIndex()).value((int)inst.classValue())+","+(total / count);
			try {
				documentEvaluations.add(Distractorless.evalCSV(analysisString+docString,thresh));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try{
		for (Evaluation e : documentEvaluations)
			System.out.println(e.toSummaryString()+"\n"+e.toClassDetailsString()+"\n"+e.toMatrixString());
		} catch (Exception e){
			
		}
	}

	@Override
	public String getResultString() {
		return "Analysis Data:\n"+analysisString+"\n-----\nResults:\n"+resultsString;
	}
	
	/**
	 * maybe iterate over analysisString, take the dist (last value) and average them?
	 * Perhaps include an extra parameter to include anything reduce/increase the threshold
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

	private void runAnalysis(){
		
		//for all pairs of instances
		for (int i = 0; i < trainingInstances.numInstances(); i++){
			Instance instI = trainingInstances.get(i);
			for (int j = 0; j<trainingInstances.numInstances(); j++){
				Instance instJ = trainingInstances.get(j);
				
				//if they are not the same instance
				if (instI != instJ){
					//if they are by the same author
					if(instI.classValue() == instJ.classValue()){
						analysisString+=String.format(
								instI.attribute(instI.classIndex()).value((int)instI.classValue())+","+
								instJ.attribute(instJ.classIndex()).value((int)instJ.classValue())+","+
								calculateCosineDistance(instI.toDoubleArray(),instJ.toDoubleArray())+"\n"
								);
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private double calculateCosineDistance(double[] a, double[] b){
		Double dot = 0.0;
		Double normA = 0.0;
		Double normB = 0.0;
		int n = a.length;
		if (a.length != b.length){
			System.out.println("Vectors are of different length! This is going to get messy...");
			if (b.length < a.length)
				n = b.length;
		}
		
		for (int i = 0; i < n; i++){
			if (i == trainingInstances.classIndex())
				continue;
			else {
				dot += a[i]*b[i];
				normA += Math.pow(a[i],2);
				normB += Math.pow(b[i],2);
			}
		}
		
		return 1-(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
	}
	
	
	//TODO adapt/remove this if needed
	/**
	 * Generate result csv from the given csv file path and write to new csv
	 * with same name with the suffix "_res". Results include: TP, TN, FP, FN,
	 * precision, recall, f-measure and accuracy.
	 */
	public void genResCSV(double min, double max, int divFactor) throws Exception
	{
		resultsString+=String.format("threshold,tp,tn,fp,fn,precision,recall,f-measure,accuracy\n");
		double threshold;
		for (int t = ((int) (min * divFactor)); t <= ((int)(max * divFactor)); t++)
		{
			threshold = ((double) t)/(divFactor * 1.0);
			Evaluation eval = Distractorless.evalCSV(analysisString, threshold);
			resultsString+=String.format(
					threshold + "," +
							eval.truePositiveRate(0) + "," +
							eval.trueNegativeRate(0) + "," +
							eval.falsePositiveRate(0) + "," +
							eval.falseNegativeRate(0) + "," +
							eval.precision(0) + "," +
							eval.recall(0) + "," +
							eval.fMeasure(0) + "," +
							(eval.pctCorrect() / 100)+"\n");
		}
	}
	
}

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
	private List<Evaluation> documentEvaluations; //stores the weka Evaluation objects for the verification
	
	private Instances trainingInstances; //known documents
	private Instances testingInstances; //documents to be verified
	private double thresholdModifier; //the modifier to be applied to the threshold
	
	public DistractorlessCV(Instances tri, Instances tei, double modifier) {
		
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

		try {
			for (Evaluation e : documentEvaluations)
				System.out.println();
		} catch (Exception e) {

		}
	}

	public String getAnalysisString(){
		return analysisString;
	}
	
	@Override
	public String getResultString() {
		String s = "";
		for (Evaluation e : documentEvaluations) {
			try {
				s += e.toSummaryString() + "\n" + e.toClassDetailsString() + "\n" + e.toMatrixString() + "\n";
			} catch (Exception exc) {

			}
		}
		return s;
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
	
}

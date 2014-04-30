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
	
	public DistractorlessCV(ProblemSet p, Instances tri, Instances tei) {
		ps=p;
		trainingInstances = tri;
		testingInstances = tei;
		resultsString = "";
		analysisString = "";
		
	}

	@Override
	public void verify() {

		// ---------------------------------------------------------------------
		// configuration
		// ---------------------------------------------------------------------
		
		String[] types = new String[]{
				//"char",
				"word"
		};
		// map to hold min, max and step (int this order)
		Map<String,int[]> Ns = new HashMap<>();
		Ns.put("char", new int[]{
				1,
				20,
				1
		});
		Ns.put("word", new int[]{
				3,
				3,
				1
		});
		
		// ---------------------------------------------------------------------
		// set environment
		// ---------------------------------------------------------------------
		
		for (String type: types)
		{
			int[] bounds = Ns.get(type);
			for (int N = bounds[0]; N <= bounds[1]; N += bounds[2])
			{

				//TODO ////////
				//We need to swap this out with the cfd somehow
				// event driver
				EventDriver ed = null;

				// chars
				if (type.equals("char"))
				{
					CharacterNGramEventDriver charGrams =
							new CharacterNGramEventDriver();
					charGrams.setN(N);
					charGrams.setParameter("N", N);
					ed = charGrams;
				}
				else if (type.equals("word"))
				{
					// words
					WordNGramEventDriver wordGrams = new WordNGramEventDriver();
					wordGrams.setN(N);
					wordGrams.setParameter("N", N);
					ed = wordGrams;
				}
				//////////////
				
				
				// centroid driver
				CentroidDriverSD centroidCosine = new CentroidDriverSD();
				centroidCosine.setAdjustByAuthorAvgInnerDist(true);
				centroidCosine.setUseFeatureSD(true);
				centroidCosine.setDistance(new HistogramDistance());
				
				// canonicizers
				List<Canonicizer> canonicizers = new LinkedList<>();
				canonicizers.add(new NormalizeWhitespace());
				canonicizers.add(new UnifyCase());
				// analyzer
				AnalysisDriver ad = centroidCosine;

				// ---------------------------------------------------------------------
				// run analysis
				// ---------------------------------------------------------------------

				try {
					genDistsCSV(ps,canonicizers,ed,ad);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// ---------------------------------------------------------------------
				// run on different thresholds
				// ---------------------------------------------------------------------
				
				try {
					genResCSV(
							-0.1,
							0.8,
							1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String getResultString() {
		return "Analysis Data:\n"+analysisString+"\n-----\nResults:\n"+resultsString;
	}

	/**
	 * Prepares all author-vs-self and author-vs-others verification problems
	 * from the given problem set, using:
	 * <ul>
	 * <li>author-vs-self: leave-1-out validation</li>
	 * <li>author-vs-others: straight-forward verification</li>
	 * <ul>
	 * Prints all results into a csv in the given path.
	 */
	public void genDistsCSV(
			ProblemSet ps,
			List<Canonicizer> canonicizers,
			EventDriver eventDriver,
			AnalysisDriver analysisDriver)
					throws Exception
	{
		// write header
		//mps.println("problem,test_path,test_author,train_author,dist");
		analysisString+=String.format("problem,test_author,train_author,dist\n");
		List<Document> documents, docs;
		Document doc;
		
		// =====================================================================
		// TEST #1: author leave-one-out on own documents
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
				
				// run analysis and update evaluation
				runAnalysisPrintCSVResults(
						"author_on_self",
						canonicizers,
						eventDriver,
						analysisDriver,
						documents);
			}
		}
		/*
		 * TODO use the runAnalysis / leave oen out results to pick the desired threshold value
		 * To accomplish this, allow the Verifier to have a target TP or FP rate, and then aim to hit that rate while max/minning other stats
		 * provide a method which parses the "resultsString" picks the appropriate threshold, then runs a single verification on the unknown document.
		 */
		Double thresh = calculateDesiredThreshold();
		for (Document d : ps.getAllTestDocs()){
			String docString = "";
			//extract features / get the "analysisString" for this document
			//call the Distractorless method "evalCSV()" with the analysisString for this document and the desired threshold
			//save results to some object
		}
			
		
		
		//TODO Let's ignore this for now. Ideally we want to separate
		//these into separate things
		// =====================================================================
		// TEST #2: author train-test on others
		// =====================================================================
		/*
		for (String trainAuthor: ps.getAuthors())
		{
			docs = ps.getTrainDocs(trainAuthor);
			
			// prepare train docs
			documents = new LinkedList<>();
			for (Document d: docs)
				documents.add(new Document(
						d.getFilePath(),
						d.getAuthor(),
						d.getTitle()));
			
			// prepare test docs
			for (String testAuthor: ps.getAuthors())
			{
				// skip train author
				if (testAuthor.equals(trainAuthor))
					continue;
				
				docs = ps.getTrainDocs(testAuthor);
				for (Document d: docs)
					documents.add(new Document(
							d.getFilePath(),
							null,
							"Correct: " + d.getAuthor()));
			}
			
			// run analysis and update evaluation
			runAnalysisPrintCSVResults(
					"author_on_others",
					canonicizers,
					eventDriver,
					analysisDriver,
					documents);
		} */
	}
	
	/**
	 * TODO figure out how to get the best threshold
	 * maybe iterate over analysisString, take the dist (last value)
	 * and average them?
	 * Perhaps include an extra parameter to include anything reduce/increase the threshold
	 * 
	 * @return
	 */
	private double calculateDesiredThreshold(){
		String aCopy = new String(analysisString);
		double cumulative = 0.0;
		int count = 0;
		String[] line;
		Scanner s = new Scanner(aCopy);

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
	
	/**
	 * Generate result csv from the given csv file path and write to new csv
	 * with same name with the suffix "_res". Results include: TP, TN, FP, FN,
	 * precision, recall, f-measure and accuracy.
	 */
	public void genResCSV(double min, double max, int divFactor) throws Exception
	{
		resultsString+=String.format("threshold,tp,tn,fp,fn,precision,recall,f-measure,accuracy\n");
		double threshold;
		//for (int t = 0; t <= numIters; t++)
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

	/**
	 * Runs analysis on the given problem set and prints measured distances
	 * into given multiple print stream.
	 */
	public void runAnalysisPrintCSVResults(
			String problem,
			List<Canonicizer> canonicizers,
			EventDriver eventDriver,
			AnalysisDriver analysisDriver,
			List<Document> documents) throws Exception
	{
		API api = new API();
				
		for(Document document : documents){
			api.addDocument(document);
		}
		
		// configure API
		for (Canonicizer c: canonicizers) {
			api.addCanonicizer(c);
		}
		/*
		for (int i = 0; i < cfd.numOfFeatureDrivers(); i++){
			FeatureDriver fd = cfd.featureDriverAt(i);
			api.addEventDriver(fd.getUnderlyingEventDriver());
		}*/
		api.addEventDriver(eventDriver);		
		api.addAnalysisDriver(analysisDriver);

		// analyze
		api.execute();

		List<Document> knownDocuments = new ArrayList<Document>();
		List<Document> unknownDocuments = new ArrayList<Document>();
		for (Document document : documents) {
			if (document.isAuthorKnown()) {
				knownDocuments.add(document);
			} else {
				unknownDocuments.add(document);
			}
		}

		List<Pair<String, Double>> res;
		for (Document document: unknownDocuments)
		{
			res = document.getResults().values().iterator().next()
					.values().iterator().next();

			for (Pair<String, Double> pair: res){
				analysisString+=String.format(
						problem + "," +
						document.getFilePath() + "," +
						document.getTitle().split(" ")[1].trim() + "," +
						pair.getFirst() + "," +
						pair.getSecond()+"\n");
			}
		}
	}
	
}

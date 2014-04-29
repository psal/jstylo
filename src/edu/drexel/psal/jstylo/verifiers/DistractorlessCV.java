package edu.drexel.psal.jstylo.verifiers;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import edu.drexel.psal.jstylo.generics.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.generics.MultiplePrintStream;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import edu.drexel.psal.jstylo.generics.Verifier;

public class DistractorlessCV extends Verifier{

	private String resultsString;
	private String savePath;
	
	public DistractorlessCV(ProblemSet p,CumulativeFeatureDriver cf,String s) {
		super(p,null,null,cf);
		resultsString = "";
		savePath = s;
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

				// csv path
				String csvPath = savePath;
				
				// ---------------------------------------------------------------------
				// run analysis
				// ---------------------------------------------------------------------

				try {
					genDistsCSV(ps,canonicizers,ed,ad,csvPath);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// ---------------------------------------------------------------------
				// run on different thresholds
				// ---------------------------------------------------------------------
				
				try {
					genResCSV(csvPath,
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
		return resultsString;
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
			AnalysisDriver analysisDriver,
			String csvPath) throws Exception
	{
		PrintStream csv = new PrintStream(new File(csvPath));
		MultiplePrintStream mps = new MultiplePrintStream(
				System.out,
				csv);
		// write header
		//mps.println("problem,test_path,test_author,train_author,dist");
		mps.println("problem,test_author,train_author,dist");
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
						documents,
						mps);
			}
		}
		
		// =====================================================================
		// TEST #2: author train-test on others
		// =====================================================================
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
					documents,
					mps);
		}
		csv.flush();
		csv.close();
	}
	
	/**
	 * Generate result csv from the given csv file path and write to new csv
	 * with same name with the suffix "_res". Results include: TP, TN, FP, FN,
	 * precision, recall, f-measure and accuracy.
	 */
	public void genResCSV(String csvPath, double min, double max, int divFactor) throws Exception
	{
		PrintStream csv = new PrintStream(new File(
				csvPath.replace(".csv", "_res.csv")));
		MultiplePrintStream mps = new MultiplePrintStream(
				System.out,
				csv);
		mps.println("threshold,tp,tn,fp,fn,precision,recall,f-measure,accuracy");
		double threshold;
		//for (int t = 0; t <= numIters; t++)
		for (int t = ((int) (min * divFactor)); t <= ((int)(max * divFactor)); t++)
		{
			threshold = ((double) t)/(divFactor * 1.0);
			Evaluation eval = Distractorless.evalCSV(csvPath, threshold);
			mps.println(
					threshold + "," +
							eval.truePositiveRate(0) + "," +
							eval.trueNegativeRate(0) + "," +
							eval.falsePositiveRate(0) + "," +
							eval.falseNegativeRate(0) + "," +
							eval.precision(0) + "," +
							eval.recall(0) + "," +
							eval.fMeasure(0) + "," +
							(eval.pctCorrect() / 100));
		}
		csv.flush();
		csv.close();
	}

	/**
	 * Runs analysis on the given problem set and prints measured distances
	 * into given multiple print stream.
	 */
	public static void runAnalysisPrintCSVResults(
			String problem,
			List<Canonicizer> canonicizers,
			EventDriver eventDriver,
			AnalysisDriver analysisDriver,
			List<Document> documents,
			MultiplePrintStream mps) throws Exception
	{
		API api = new API();
				
		for(Document document : documents){
			api.addDocument(document);
		}
		
		// configure API
		for (Canonicizer c: canonicizers)
			api.addCanonicizer(c);
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

			for (Pair<String, Double> pair: res)
				mps.println(
						problem + "," +
						/*document.getFilePath() + "," +*/
						document.getTitle().split(" ")[1].trim() + "," +
						pair.getFirst() + "," +
						pair.getSecond());
		}
	}
	
}

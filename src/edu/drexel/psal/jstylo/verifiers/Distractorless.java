package edu.drexel.psal.jstylo.verifiers;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.jgaap.backend.API;
import com.jgaap.generics.Document;
import com.jgaap.generics.EventCuller;
import com.jgaap.generics.EventDriver;
import com.jgaap.generics.EventSet;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Instance;
import weka.core.Instances;

public class Distractorless {

	/*
	 * These values are used to create a "fake" weka evaluation for statistical purposes.
	 * Basically, we set the options as:
	 *  "classified as author, correct,
	 *   classified as not author, correct,
	 *   classified as author, incorrect,
	 *   classified as not author, incorrect"
	 *   and then manually train create the evaluations with the correct number
	 *   of corrects/incorrects to collect a variety of useful statistics.
	 */
	public static String AUTHOR = "AUTHOR";
	public static String NOT_AUTHOR = "NOT_AUTHOR";
	public static String instsStr =
			"@relation stub\n" +
			"\n" +
			"@attribute f numeric\n" +
			"@attribute class {" + AUTHOR + "," + NOT_AUTHOR + "}\n" + 
			"\n" +
			"@data\n" +
			"0,AUTHOR\n" +		// author good
			"1,NOT_AUTHOR\n" +	// not author good 
			"1,AUTHOR\n" +		// author bad
			"0,NOT_AUTHOR\n";	// not author bad
	public static Instances insts;
	public static Instance
		AUTHOR_GOOD,
		AUTHOR_BAD,
		NOT_AUTHOR_GOOD,
		NOT_AUTHOR_BAD;
	public static SMO SMO = new SMO();
	
	/*
	 * Build the classifier with the instances
	 */
	static
	{
		try {
			insts = new Instances(new StringReader(instsStr));
		} catch (IOException e) {
			System.err.println("Error initializing evaluation stubs!!!");
		}
		insts.setClassIndex(1);
		AUTHOR_GOOD = insts.instance(0);
		NOT_AUTHOR_GOOD = insts.instance(1);
		AUTHOR_BAD = insts.instance(2);
		NOT_AUTHOR_BAD = insts.instance(3);
		insts.delete(3);
		insts.delete(2);
		
		try {
			SMO.buildClassifier(insts);
		} catch (Exception e) {
			System.err.println("Error training classifier!!!");
		}
	}
	
	/////////////////////////////

	/**
	 * Reads the CSV file in the given path and evaluates the results with the
	 * given threshold.
	 */
	public static Evaluation evalCSV(String analysisString, double threshold)
			throws Exception
	{
		Evaluation eval = new Evaluation(insts);
		
		Scanner scan = new Scanner(analysisString);
		String[] line;
		String testAuthor, trainAuthor;
		double dist;
		
		// skip header
		scan.nextLine();
		
		while (scan.hasNext())
		{
			line = scan.nextLine().split(",");
			
			/*
			testAuthor = line[2];
			trainAuthor = line[3];
			dist = Double.parseDouble(line[4]);
			//TODO swap this depending on analysis 
			*/
			trainAuthor = line[0];
			testAuthor = line[1];
			dist = Double.parseDouble(line[2]);
			
			updateEval(eval, testAuthor, trainAuthor, dist, threshold);
		}
		scan.close();
		
		return eval;
	}
	
	/**
	 * updates the input evaluation according to the given parameters.
	 */
	public static void updateEval(Evaluation eval, String testAuthor,
			String trainAuthor, double dist, double threshold) throws Exception
	{
		if (testAuthor.equals(trainAuthor)) {
			if (dist < threshold)
				incTP(eval);
			else
				incFN(eval);
		} else {
			if (dist < threshold)
				incFP(eval);
			else
				incTN(eval);
		}
	}
	
	public static void incTP(Evaluation eval) throws Exception
	{
		eval.evaluateModelOnce(SMO, AUTHOR_GOOD);
	}
	
	public static void incTN(Evaluation eval) throws Exception
	{
		eval.evaluateModelOnce(SMO, NOT_AUTHOR_GOOD);
	}
	
	public static void incFP(Evaluation eval) throws Exception
	{
		eval.evaluateModelOnce(SMO, NOT_AUTHOR_BAD);
	}
	
	public static void incFN(Evaluation eval) throws Exception
	{
		eval.evaluateModelOnce(SMO, AUTHOR_BAD);
	}
	
	
	public static void loadCanonicizeEventify(API api) throws Exception
	{
		List<Document> documents = api.getDocuments();
		List<EventDriver> eventDrivers = api.getEventDrivers();
		for (EventDriver eventDriver: eventDrivers)
		{
			for(Document document : documents)
			{
				document.load();
				document.processCanonicizers();
				document.addEventSet(eventDriver,
						eventDriver.createEventSet(document));
				document.readStringText("");
			}
		}
	}
	
	public static void cull(API api) throws Exception
	{
		List<Document> documents = api.getDocuments();
		List<EventDriver> eventDrivers = api.getEventDrivers();
		List<EventCuller> eventCullers = api.getEventCullers();
		List<EventSet> eventSets = new ArrayList<EventSet>();
		for (EventDriver eventDriver: eventDrivers)
		{
			for (final Document document : documents) {
				if (document.getEventSets().containsKey(eventDriver)) {
					eventSets.add(document.getEventSet(eventDriver));
				}
			}
			for (EventCuller culler : eventCullers) {
				eventSets = culler.cull(eventSets);
			}
			for (Document document : documents) {
				if (document.getEventSets().containsKey(eventDriver)) {
					document.addEventSet(eventDriver, eventSets.remove(0));
				}
			}
			eventSets.clear();
		}
	}
	
}

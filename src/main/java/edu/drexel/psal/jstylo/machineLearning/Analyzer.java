package edu.drexel.psal.jstylo.machineLearning;


import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import com.jgaap.generics.Document;
import weka.classifiers.Evaluation;
import edu.drexel.psal.jstylo.generics.DataMap;

/**
 * Abstract class for analyzers - an interface to various Machine Learning libraries/algorithms.
 * Backed by a DataMap -- a JStylo specific, but fairly generic (ie only uses inbuilt java classes), data structure.
 * 
 * @author Travis Dutko
 */
public abstract class Analyzer{
	
	/* ======
	 * fields
	 * ======
	 */
    
    /**
     * The training and data
     */
	protected DataMap trainingSet;
	
	/**
	 * The testing data
	 */
	protected DataMap testSet;
	
	/**
	 * Mapping of test documents to distribution classification results for each unknown document.
	 */
	protected Map<String,Map<String, Double>> results;
	
	/**
	 * List of authors.
	 */
	protected List<String> authors;
	
	/**
	 * Array of options.
	 */
	protected String[] options;
	
	/* ============
	 * constructors
	 * ============
	 */
	
	// -- none --
	
	/* ==========
	 * operations
	 * ==========
	 */
	
	/**
	 * Classifies the given test set based on the given training set. Should update the following fields along the classification:
	 * trainingSet, testSet, results and authors.
	 * Returns list of distributions of classification probabilities per instance.
	 * @param trainingSet
	 * 		The training DataMap
	 * @param testSet
	 * 		The testing DataMap
	 * @param unknownDocs
	 * 		The list of test documents to deanonymize.
	 * @return
	 * 		The mapping from test documents to distributions of classification probabilities per document, or
	 * 		null if prepare was not previously called.
	 * 		Each result in the list is a mapping from the author to its corresponding
	 * 		classification probability.
	 */
	public abstract Map<String,Map<String, Double>> classify(
			DataMap trainingSet, DataMap testSet, List<Document> unknownDocs);
	
	/**
	 * Runs cross validation with given number of folds on the given DataMap object.
	 * @param data
	 * 		The data to run CV over.
	 * @param folds
	 * 		The number of folds to use.
	 * @param randSeed
	 * 		Random seed to be used for fold generation.
	 *  @return
	 * 		Some object containing the cross validation results (e.g. Evaluation for Weka
	 * 		classifiers CV results), or null if failed running.		
	 */
	public abstract Evaluation runCrossValidation(DataMap data, int folds, long randSeed);
	
	
	/**
	 * Runs a relaxed cross validation with given number of folds on the given DataMap object.
	 * A classification result will be considered correct if the true class is
	 * one of the top <code>relaxFactor</code> results (where classes are ranked
	 * by the classifier probability they are the class).
	 * @param data
	 * 		The data to run CV over.
	 * @param folds
	 * 		The number of folds to use.
	 * @param randSeed
	 * 		Random seed to be used for fold generation.
	 * @param relaxFactor
	 * 		The relax factor for the classification.
	 * @return
	 * 		Some object containing the cross validation results (e.g. Evaluation for Weka
	 * 		classifiers CV results), or null if failed running.		
	 */
	public abstract Evaluation runCrossValidation(DataMap data, int folds, long randSeed, int relaxFactor);
	
	/* =======
	 * getters
	 * =======
	 */
	
	/**
	 * Returns the string representation of the last classification results.
	 * @return
	 * 		The string representation of the classification results.
	 */
	public String getLastStringResults() {
		// if there are no results yet
		if (results == null)
			return "No results!";		
		
		String res = "";
		Formatter f = new Formatter();
		f.format("%-14s |", "doc \\ author");
		
		List<String> actualAuthors = new ArrayList<String>(authors);
		
		for (String author: actualAuthors)
			f.format(" %-14s |",author);

		res += f.toString()+"\n";
		for (int i=0; i<actualAuthors.size(); i++)
			res += "-----------------";
		res += "----------------\n";
		
		for (String testDocTitle: results.keySet()) {
			f = new Formatter();
			f.format("%-14s |", testDocTitle);

			Map<String,Double> currRes = results.get(testDocTitle);	
			
			String resAuthor = "";
			double maxProb = 0, oldMaxProb;

			for (String author: currRes.keySet()) { 
				oldMaxProb = maxProb;
				maxProb = Math.max(maxProb, currRes.get(author).doubleValue());
				if (maxProb > oldMaxProb)
					resAuthor = author;
			}
			
			for (String author: actualAuthors) {
				
				char c;
				if (author.equals(resAuthor))
					c = '+';
				else c = ' ';
				try{
					f.format(" %2.6f %c     |",currRes.get(author).doubleValue(),c);
				} catch (NullPointerException e){
					
				}
			}
			res += f.toString()+"\n";
		}
		f.close();
		res += "\n";
		return res;
	}
	
	/**
	 * 
	 * @param train
	 * @param test
	 * @return
	 * @throws Exception
	 */
	public abstract Evaluation getTrainTestEval(DataMap train, DataMap test) throws Exception ;
	
	/**
	 * Returns the last training DataMap that was used for classification.
	 * @return
	 * 		The last training DataMap that was used for classification.
	 */
	public DataMap getLastTrainingSet() {
		return trainingSet;
	}
	
	/**
	 * Returns the last test DataMap that was used for classification.
	 * @return
	 * 		The last test DataMap that was used for classification.
	 */
	public DataMap getLastTestSet() {
		return testSet;
	}
	
	/**
	 * Returns the last list of author names.
	 * @return
	 * 		The last list of author names.
	 */
	public List<String> getLastAuthors() {
		return authors;
	}
	
	/**
	 * Returns the last classification results or null if no classification was applied.
	 * @return
	 * 		The classification results or null if no classification was applied.
	 */
	public Map<String,Map<String, Double>> getLastResults() {
		return results;
	}
	
	/**
	 * Returns an array containing each of the analyzer's/classifier's options
	 * @return the arguments the analyzer has or null if it doesn't have any
	 */
	public String[] getOptions(){
			return options;
	}
	
	/**
	 * Sets the option string
	 * @param ops array of strings (the arguments) for the analyzer/classifier
	 */
	public void setOptions(String[] ops){
		options = ops;
	}
	
	/**
	 * An array of 1-2 sentences per index describing each option the analyzer has and the flag used to invoke each argument.
	 * It is essential that each the flag comes prior to the description and that in between them, an \<ARG\> tag appears.
	 * The arg parser expects the description in this format, and failing to comply to it will result in the inability to edit the args.
	 * Example:
	 * -C\<ARG\>Enables some function c such that...
	 * 
	 * @return a description corresponding to each option the analyzer/classifier has
	 */
	public abstract String[] optionsDescription();
	
	/**
	 * A string describing the analyzer. Should be formatted and ready for display as well as be somewhat detailed.
	 * @return the string describing how the analyzer/classifier functions and its benefits/drawbacks
	 */
	public abstract String analyzerDescription();
	
	/**
	 * Returns a short identifier for the Analyzer
	 * @return the name of the analyzer/classifier being used
	 */
	public abstract String getName();
	
}

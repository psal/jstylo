package edu.drexel.psal.jstylo.machineLearning;

import java.util.List;

import com.jgaap.generics.Document;
import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.ExperimentResults;

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
	//protected Map<String,Map<String, Double>> results;
	
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
	public abstract ExperimentResults classifyWithUnknownAuthors(
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
	public abstract ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed);
	
	
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
	public abstract ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed, int relaxFactor);
	
	/* =======
	 * getters
	 * =======
	 */
	
	/**
	 * 
	 * @param train
	 * @param test
	 * @return
	 * @throws Exception
	 */
	public abstract ExperimentResults classifyWithKnownAuthors(DataMap train, DataMap test) throws Exception ;
	
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
	
	/**
	 * Returns metric string internal to this analyzer
	 * @return
	 */
	public abstract String getExperimentMetrics();
	
}

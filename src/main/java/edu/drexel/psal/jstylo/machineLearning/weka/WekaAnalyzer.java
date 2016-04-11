package edu.drexel.psal.jstylo.machineLearning.weka;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocResult;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jgaap.generics.Document;

import weka.classifiers.*;
import weka.core.*;

/**
 * Designed as Weka-classifier-based analyzer. 
 * 
 * @author Ariel Stolerman
 */
public class WekaAnalyzer extends Analyzer {

	/* ======
	 * fields
	 * ======
	 */
	
	/**
	 * The underlying Weka classifier to be used.
	 */
	private Classifier classifier;
	
	/**
	 * Data to train on
	 */
	private Instances trainingInstances;
	
	/**
	 * Data to test on
	 */
	private Instances testingInstances;
	
	/**
	 * Document titles
	 */
	private List<String> documentTitles;
	
	/* ============
	 * constructors
	 * ============
	 */
	
	/**
	 * Default constructor with SMO SVM as default classifier.
	 */
	public WekaAnalyzer() {
		classifier = new weka.classifiers.functions.SMO();
	}
	
	public WekaAnalyzer(Classifier classifier) {
		this.classifier = classifier;
	}
	
	public WekaAnalyzer(Object obj){
		this.classifier = (Classifier) obj;
	}
	
	/* ==========
	 * operations
	 * ==========
	 */
		
	/**
	 * Trains the Weka classifier using the given training set, and then classifies all instances in the given test set.
	 * Returns list of distributions of classification probabilities per instance.
	 * @param trainMap
	 * 		The datamap to convert into Instances to train on
	 * @param testMap
	 * 		The datamap to convert into Instances to test on
	 * @param unknownDocs
	 * 		The test documents to be deanonymized.
	 * @return
	 * 		The mapping of test documents to distributions of classification probabilities per instance, or null if prepare was
	 * 		not previously called. Each result in the list is a mapping from the author to its corresponding
	 * 		classification probability.
	 */
	@Override
	public ExperimentResults classify(DataMap trainMap,	
			DataMap testMap, List<Document> unknownDocs) {
	    documentTitles = testMap.getDocumentTitles();
		trainingInstances = WekaUtils.instancesFromDataMap(trainMap);					
		testingInstances = WekaUtils.instancesFromDataMap(testMap);
		ExperimentResults results = new ExperimentResults();
		// initialize authors (extract from training set)
		List<String> authors = new ArrayList<String>();
		Attribute authorsAttr = trainingInstances.attribute("authorName");
		for (int i=0; i< authorsAttr.numValues(); i++)
			authors.add(i,authorsAttr.value(i));
		this.authors = authors;
		
		int numOfInstances = testingInstances.numInstances();
		int numOfAuthors = authors.size();
		
		Map<String,Map<String, Double>> res = new HashMap<String,Map<String,Double>>(numOfInstances);
		for (int i=0; i<numOfInstances; i++)
			res.put(unknownDocs.get(i).getTitle(), new HashMap<String,Double>(numOfAuthors));
		
		// train classifier
		trainingInstances.setClass(authorsAttr);
		try {
			classifier.buildClassifier(trainingInstances);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		// classify test cases
		Map<String,Double> map;
		double[] currRes;
		for (int i=0; i<testingInstances.numInstances(); i++) {
			Instance test = testingInstances.instance(i);

			test.setDataset(trainingInstances);

			map = res.get(unknownDocs.get(i).getTitle());
			try {
				currRes = classifier.distributionForInstance(test);
				for (int j=0; j<numOfAuthors; j++) {
					map.put(authors.get(j), currRes[j]);
				}
				//FIXME something around here isn't working properly
				
				results.addDocResult(new DocResult(documentTitles.get(i),map));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return results;
	}
	
	/**
	 * Runs cross validation with given number of folds on the given Instances object.
	 * @param data
	 * 		The data to run CV over.
	 * @param folds
	 * 		The number of folds to use.
	 * @param randSeed
	 * 		Random seed to be used for fold generation.
	 *  @return
	 * 		The evaluation object with cross-validation results, or null if failed running.
	 */
	@Override
	//FIXME see FullAPI for current bug documentation
	public ExperimentResults runCrossValidation(DataMap datamap, int folds, long randSeed) {
	    documentTitles = datamap.getDocumentTitles();
	    Instances data = WekaUtils.instancesFromDataMap(datamap);
		// setup
		data.setClass(data.attribute("authorName"));
		
		Instances randData = new Instances(data);
		Random rand = new Random(randSeed);
		randData.randomize(rand);
		randData.stratify(folds);

		// run CV
		Evaluation eval = null;
		try {
			eval = new Evaluation(randData);
			for (int n = 0; n < folds; n++) {
				Instances train = randData.trainCV(folds, n);
				Instances test = randData.testCV(folds, n);
				// build and evaluate classifier
				Classifier clsCopy = Classifier.makeCopy(classifier);
				clsCopy.buildClassifier(train);
				eval.evaluateModel(clsCopy, test);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return WekaUtils.resultsFromEvaluation(eval, data.attribute(data.numAttributes()-1).toString(),documentTitles);
	}
	
	protected String resultsString(Evaluation eval){
		String results = "";
		
		results+=eval.toSummaryString(false)+"\n";
		try {
			results+=eval.toClassDetailsString()+"\n";
			results+=eval.toMatrixString()+"\n";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return results;
	}
	
	@Override
	public ExperimentResults getTrainTestEval(DataMap trainMap, DataMap testMap) throws Exception{
	    documentTitles = testMap.getDocumentTitles();
	    Instances train = WekaUtils.instancesFromDataMap(trainMap);
	    Instances test = WekaUtils.instancesFromDataMap(testMap);
        test.setClassIndex(test.numAttributes()-1);
        train.setClassIndex(train.numAttributes()-1);
		Classifier cls = Classifier.makeCopy(classifier);
		cls.buildClassifier(train);
		Evaluation eval = new Evaluation(train);
		test.setClassIndex(test.numAttributes()-1);
		eval.evaluateModel(cls,test);
		return WekaUtils.resultsFromEvaluation(eval,train.attribute(train.numAttributes()-1).toString(),documentTitles);
	}
	
	@Override
	public ExperimentResults runCrossValidation(DataMap datamap, int folds, long randSeed,
			int relaxFactor) {
	    
		if (relaxFactor==1)
			return runCrossValidation(datamap,folds,randSeed);
		
		documentTitles = datamap.getDocumentTitles();
		
		Instances data = WekaUtils.instancesFromDataMap(datamap);
		
		// setup
		data.setClass(data.attribute("authorName"));
		Instances randData = new Instances(data);
		Random rand = new Random(randSeed);
		randData.randomize(rand);
		randData.stratify(folds);

		// run CV
		RelaxedWekaEvaluation eval = null;
		try {
			eval = new RelaxedWekaEvaluation(randData, relaxFactor);
			for (int n = 0; n < folds; n++) {
				Instances train = randData.trainCV(folds, n);
				Instances test = randData.testCV(folds, n);
				// build and evaluate classifier
				Classifier clsCopy =  Classifier.makeCopy(classifier);
				clsCopy.buildClassifier(train);
				eval.evaluateModel(clsCopy, test);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return WekaUtils.resultsFromEvaluation(eval,data.attribute(data.numAttributes()-1).toString(),documentTitles); 
	}
	
	/**
	 * Runs cross validation with given number of folds on the given Instances object.
	 * Uses 0 as default random seed for fold generation.
	 * @param data
	 * 		The data to run CV over.
	 * @param folds
	 * 		The number of folds to use.
	 * @return
	 * 		The evaluation object with cross-validation results, or null if did not succeed running.
	 */
	public ExperimentResults runCrossValidation(DataMap datamap, int folds) {
		long randSeed = 0;
		return runCrossValidation(datamap, folds, randSeed);
	}
	
	
	/* =======
	 * getters
	 * =======
	 */
	
	/**
	 * Returns the underlying classifier.
	 * @return
	 * 		The underlying Weka classifier.
	 */
	public Classifier getClassifier() {
		return classifier;
	}
	
	/**
	 * Returns the weka classifier's args
	 */
	@Override
	public String[] getOptions(){
		return ((OptionHandler) classifier).getOptions();
	}
	
	/**
	 * Returns the analyzer's classifier--so the classifier's name.
	 */
	@Override
	public String getName(){
		return "WekaAnalyzer with "+classifier.getClass().getName()+" classifier";
	}
	
	/**
	 * Produces the string array of the flags and descriptions for all of the arguments the classifier can take.
	 */
	@Override
	public String[] optionsDescription() {
		ArrayList<String> optionsDesc= new ArrayList<String>();
		String[] optionsDescToReturn = null;
		
		@SuppressWarnings("unchecked")
		Enumeration<Option> opts = ((OptionHandler) classifier).listOptions();
		Option nextOpt = null;
		while (opts.hasMoreElements()){
			nextOpt = opts.nextElement();
			optionsDesc.add(nextOpt.name()+"<ARG>"+nextOpt.description());
		}
		
		optionsDescToReturn = new String[optionsDesc.size()];
		
		int i=0;
		for (String s: optionsDesc){
			optionsDescToReturn[i]=s;
			i++;
		}
		
		return optionsDescToReturn;
	}
	
	/** 
	 * returns the description of the analyzer itself.
	 */
	@Override
	public String analyzerDescription() {
		return ((TechnicalInformationHandler) classifier).getTechnicalInformation().toString();
	}
	
	/* =======
	 * setters
	 * =======
	 */
	
	/**
	 * Sets the underlying Weka classifier to the given one.
	 * @param classifier
	 * 		The Weka classifier to set to.
	 */
	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	/**
	 * Sets the weka classifier's args.
	 */
	@Override
	public void setOptions(String[] ops){
		try {
			((OptionHandler) classifier).setOptions(ops);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

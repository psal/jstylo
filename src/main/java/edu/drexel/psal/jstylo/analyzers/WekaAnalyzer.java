package edu.drexel.psal.jstylo.analyzers;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import edu.drexel.psal.jstylo.machineLearning.AnalyzerTypeEnum;
import edu.drexel.psal.jstylo.machineLearning.RelaxedEvaluation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
	private Instances trainingInstances;
	private Instances testingInstances;
	
	//TODO - Important
	private Instances instancesFromDataMap(DataMap datamap){
	    Instances instances = null;
	    FastVector attributes = createFastVector(datamap.getFeatures(),datamap.getDataMap().keySet());
	    int numfeatures = attributes.size();
	    instances = new Instances("Instances",attributes,datamap.numDocuments());
	    
	    //for each author...
	    for (String author : datamap.getDataMap().keySet()){
	        ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>> authormap 
	            = datamap.getDataMap().get(author);
	        
	        //for each document...
	        for (String doctitle : authormap.keySet()){
	            
	            Instance instance = new SparseInstance(numfeatures);
	            ConcurrentHashMap<Integer,Double> documentData = authormap.get(doctitle);
	            
	            //for each index we have a value for 
	            for (Integer index : documentData.keySet()){
	                instance.setValue((Attribute)attributes.elementAt(index), documentData.get(index));
	            }
	            instance.setValue((Attribute)attributes.elementAt(attributes.size()-1), author);
	        }
	    }
	    
	    return instances;
	}
	
	private FastVector createFastVector(List<String> features, Set<String> authors){
	    FastVector fv = new FastVector(features.size()+1);
	    
	    for (int i=0; i<features.size(); i++){
	        fv.addElement(new Attribute(features.get(i),i));
	    }
	    
	    //author names
	    FastVector authorNames = new FastVector();
	    List<String> authorsSorted = new ArrayList<String>(authors.size());
	    authorsSorted.addAll(authors);
	    
	    for (String author : authorsSorted){
	        System.out.println("Name: "+author);
	        authorNames.addElement(author);
	    }
	    Attribute authorNameAttribute = new Attribute("authorName", authorNames);
	    
	    fv.addElement(authorNameAttribute);
	    
	    return fv;
	}
	
	/* ============
	 * constructors
	 * ============
	 */
	
	/**
	 * Default constructor with SMO SVM as default classifier.
	 */
	public WekaAnalyzer() {
		classifier = new weka.classifiers.functions.SMO();
		type = AnalyzerTypeEnum.WEKA_ANALYZER;
	}
	
	public WekaAnalyzer(Classifier classifier) {
		this.classifier = classifier;
		type = AnalyzerTypeEnum.WEKA_ANALYZER;
	}
	
	public WekaAnalyzer(Object obj){
		this.classifier = (Classifier) obj;
		type = AnalyzerTypeEnum.WEKA_ANALYZER;
	}
	
	/* ==========
	 * operations
	 * ==========
	 */
		
	/**
	 * Trains the Weka classifier using the given training set, and then classifies all instances in the given test set.
	 * Returns list of distributions of classification probabilities per instance.
	 * @param trainingInstances
	 * 		The Weka Instances dataset of the training instances.
	 * @param testingInstances
	 * 		The Weka Instances dataset of the test instances.
	 * @param unknownDocs
	 * 		The test documents to be deanonymized.
	 * @return
	 * 		The mapping of test documents to distributions of classification probabilities per instance, or null if prepare was
	 * 		not previously called. Each result in the list is a mapping from the author to its corresponding
	 * 		classification probability.
	 */
	@Override
	public Map<String, Map<String, Double>> classify(DataMap trainMap,	
			DataMap testMap, List<Document> unknownDocs) {
		trainingInstances = instancesFromDataMap(trainMap);					
		testingInstances = instancesFromDataMap(testMap);
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		results = res;
		return res;
	}
	
	/**
	 * Trains the Weka classifier using the given training set, and then classifies all instances in the given test set.
	 * Before classifying, it removes the second attribute in both sets, that is the document title. This should be used when
	 * the WekaInstancesBuilder object used to create the sets has hasDocNames set to true.
	 * Returns list of distributions of classification probabilities per instance.
	 * @param trainingInstances
	 * 		The Weka Instances dataset of the training instances.
	 * @param testingInstances
	 * 		The Weka Instances dataset of the test instances.
	 * @param unknownDocs
	 * 		The test documents to be deanonymized.
	 * @return
	 * 		The list of distributions of classification probabilities per instance, or null if prepare was
	 * 		not previously called. Each result in the list is a mapping from the author to its corresponding
	 * 		classification probability.
	 */
	public Map<String, Map<String, Double>> classifyRemoveTitle(DataMap trainingMap,
			DataMap testingMap, List<Document> unknownDocs) {
	    //FIXME this should not be needed, verify that.
	    
	    /*
	    Instances trainingInstances = instancesFromDataMap(trainingMap);
	    Instances testingInstances = instancesFromDataMap(testingMap);
		// remove titles
		Remove remove = new Remove();
		remove.setAttributeIndicesArray(new int[]{1});
		try {
			remove.setInputFormat(trainingInstances);
			this.trainingInstances = Filter.useFilter(trainingInstances, remove);
			this.testingInstances = Filter.useFilter(testingInstances, remove);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}*/
		
		return null;
		//return classify(this.trainingInstances,this.testingInstances,unknownDocs);
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
	public Evaluation runCrossValidation(DataMap datamap, int folds, long randSeed) {
	    Instances data = instancesFromDataMap(datamap);
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
		
		return eval;
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
	public Evaluation getTrainTestEval(DataMap trainMap, DataMap testMap) throws Exception{
	    Instances train = instancesFromDataMap(trainMap);
	    Instances test = instancesFromDataMap(testMap);
		Classifier cls = Classifier.makeCopy(classifier);
		cls.buildClassifier(train);
		Evaluation eval = new Evaluation(train);
		test.setClassIndex(test.numAttributes()-1);
		eval.evaluateModel(cls,test);
		return eval;
	}
	
	@Override
	public Evaluation runCrossValidation(DataMap datamap, int folds, long randSeed,
			int relaxFactor) {
	    
		if (relaxFactor==1)
			return runCrossValidation(datamap,folds,randSeed);
		
		Instances data = instancesFromDataMap(datamap);
		
		// setup
		data.setClass(data.attribute("authorName"));
		Instances randData = new Instances(data);
		Random rand = new Random(randSeed);
		randData.randomize(rand);
		randData.stratify(folds);

		// run CV
		RelaxedEvaluation eval = null;
		try {
			eval = new RelaxedEvaluation(randData, relaxFactor);
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

		return eval; 
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
	public Evaluation runCrossValidation(DataMap datamap, int folds) {
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
	@Override
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
		return classifier.getClass().getName();
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

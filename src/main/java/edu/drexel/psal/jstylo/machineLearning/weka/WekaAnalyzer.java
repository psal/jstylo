package edu.drexel.psal.jstylo.machineLearning.weka;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;

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
	
	//FIXME make private after all else is done
	public static Instances instancesFromDataMap(DataMap datamap){
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
	            instances.add(instance);
	        }
	    }
	    
	    return instances;
	}
	
    public static DataMap datamapFromInstances(Instances instances,boolean hasTitle){
        
        //get all feature labels
        List<String> features = new ArrayList<String> (instances.numAttributes());
        for (int i = 0; i<instances.numAttributes(); i++){
            if (!instances.attribute(i).name().equalsIgnoreCase("authorName"))
                features.add(i, instances.attribute(i).name());
        }
        
        DataMap datamap = new DataMap("developingMap",features);
        
        for (int i = 0; i<instances.numInstances(); i++){
            Instance document = instances.instance(i);
            
            String author = document.stringValue(document.numAttributes()-1);
            String doctitle = "post-processed";
            if (hasTitle)
                doctitle = document.stringValue(0);
            
            ConcurrentHashMap<Integer,Double> docMap = new ConcurrentHashMap<Integer,Double>();
            for (int j = 1; j<document.numAttributes()-1; j++){
                
                //shifting left 1 for generics sake--most ML libraries don't use the 0th as doc title.
                docMap.put(j-1, document.value(j));
            }

            datamap.addDocumentData(author, doctitle, docMap);
        }

        return datamap;
    }
	
	private static FastVector createFastVector(Map<Integer,String> features, Set<String> authors){
	    FastVector fv = new FastVector(features.size()+1);
	    
	    for (Integer i : features.keySet()){
	        fv.addElement(new Attribute(features.get(i),i));
	    }
	    
	    //author names
	    FastVector authorNames = new FastVector();
	    List<String> authorsSorted = new ArrayList<String>(authors.size());
	    authorsSorted.addAll(authors);
	    
	    for (String author : authorsSorted){
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
        test.setClassIndex(test.numAttributes()-1);
        train.setClassIndex(train.numAttributes()-1);
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

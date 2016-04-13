package edu.drexel.psal.jstylo.generics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgaap.generics.Document;

import edu.drexel.psal.jstylo.featureProcessing.Chunker;
import edu.drexel.psal.jstylo.featureProcessing.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.featureProcessing.LocalParallelFeatureExtractionAPI;
import edu.drexel.psal.jstylo.featureProcessing.StringDocument;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import edu.drexel.psal.jstylo.machineLearning.Verifier;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;

/**
 * 
 * JStylo fullAPI Version 1.0<br>
 * 
 * A simple API for the inner JStylo functionality.<br>
 * Provides four constructors at the moment (eventually more) <br>
 * After the fullAPI is constructed, users need only call prepareInstances(),
 * (sometimes, depending on constructor) prepareAnalyzer(), and run().<br>
 * After fetch the relevant information with the correct get method<br>
 * @author Travis Dutko
 */

public class FullAPI {

    private static final Logger LOG = LoggerFactory.getLogger(FullAPI.class);
    
	/**
	 * Builder for the fullAPI class. <br>
	 *
	 * You must specify at least one of each of the following pairs:<br>
	 * psXMLPath and probSet<br>
	 * cfdXMLPath and cfd<br>
	 * classifierPath  and classifier<br>
	 * 
	 * All other parameters have the following default values:<br>
	 * numThreads = 4<br>
	 * numFolds = 10<br>
	 * type = analysisType.CROSS_VALIDATION<br>
	 * useDocTitles = false<br>
	 */
	public static class Builder{
		private String psXMLPath;
		private ProblemSet probSet;
		private String cfdXMLPath;
		private String verifierName;
		private CumulativeFeatureDriver cfd;
		private Analyzer analyzer;
		private int numFolds = 10;
		private analysisType type = analysisType.CROSS_VALIDATION;
		private boolean loadDocContents = false;
		private Preferences p = null;
		private boolean useCache = false;
		private boolean chunkDocs = false;
		
		public Builder(){
			
		}
		
		public Builder preferences(Preferences pref){
			p = pref;
			return this;
		}
		
		public Builder psPath(String psXML){
			psXMLPath = psXML;
			return this;
		}
		
		public Builder cfdPath(String cfdXML){
			cfdXMLPath = cfdXML;
			return this;
		}
		
		public Builder setAnalyzer(Analyzer analyzer){
		    this.analyzer = analyzer;
		    return this;
		}
		
		public Builder ps(ProblemSet ps){
			probSet = ps;
			return this;
		}
		
		public Builder cfd(CumulativeFeatureDriver cFeatDriver){
			cfd = cFeatDriver;
			return this;
		}

		public Builder numThreads(int nt){
			if (p == null){
				p = Preferences.buildDefaultPreferences();
			}
			p.setPreference("numCalcThreads",""+nt);
			return this;
		}
		
		public Builder numFolds(int nf){
			numFolds = nf;
			return this;
		}
		
		public Builder analysisType(analysisType at){
			type = at;
			return this;
		}
		
		public Builder useCache(boolean uc) {
			useCache = uc;
			return this;
		}
		
		public Builder chunkDocs(boolean ch){
		    chunkDocs = ch;
		    return this;
		}
		
		public Builder loadDocContents(boolean ldc){
			loadDocContents = ldc;
			return this;
		}
		
		public Builder verifierName(String v){
			verifierName = v;
			return this;
		}
		
		public FullAPI build(){
			return new FullAPI(this);
		}
		
	}
	
	///////////////////////////////// Data
	
	//which evaluation to perform enumeration
	public static enum analysisType {CROSS_VALIDATION,TRAIN_TEST_UNKNOWN,TRAIN_TEST_KNOWN};
	
	//Persistant/necessary data
	LocalParallelFeatureExtractionAPI ib; //does the feature extraction
	String classifierPath; //creates analyzer
	Analyzer analysisDriver; //does the train/test/crossVal
	analysisType selected; //type of evaluation
	int numFolds; //folds for cross val (defaults to 10)
	String verifierName; //verifier to use (can't create it right away as most need parameters
	Verifier verifier; //the verifier object
	
	
	//Result Data
	ExperimentResults experimentResults;
	
	///////////////////////////////// Constructor
	/**
	 * Constructor; the fullAPI can be built solely via a Builder.
	 * @param b the builder
	 */
	private FullAPI(Builder b){
		ib = new LocalParallelFeatureExtractionAPI();
		
		if (b.p == null){
			ib.setPreferences(Preferences.buildDefaultPreferences());
		} else {
			ib.setPreferences(b.p);
		}
		
		if (b.psXMLPath==null)
			ib.setProblemSet(b.probSet);
		else 
			ib.setProblemSet(new ProblemSet(b.psXMLPath,
					b.loadDocContents));
		
		if (b.cfdXMLPath==null)
			ib.setCumulativeFeatureDriver(b.cfd);
		else {
			try {
				ib.setCumulativeFeatureDriver(new CumulativeFeatureDriver(b.cfdXMLPath));
			} catch (Exception e) {
				LOG.error("Failed to build cfd from xml path: "+b.cfdXMLPath,e);
				e.printStackTrace();
			}
		}
		
		ib.setUseCache(b.useCache);
		ib.setLoadDocContents(b.loadDocContents);
		ib.setChunkDocs(b.chunkDocs);
		verifierName = b.verifierName;
		selected = b.type;
		numFolds = b.numFolds;
		analysisDriver = b.analyzer;
	}
	
	///////////////////////////////// Methods
	
	/**
	 * Prepares the instances objects (stored within the InstancesBuilder)
	 */
	public void prepareInstances() {

		try {
			if (ib.isUsingCache())
				ib.validateCFDCache();
			if (ib.isChunkingDocs())
			    Chunker.chunkAllTrainDocs(ib.getProblemSet());
			ib.extractEventsThreaded(); //extracts events from documents
			ib.initializeRelevantEvents(); //creates the List<EventSet> to pay attention to
			ib.initializeFeatureSet(); //creates the attribute list to base the Instances on
			ib.createTrainingDataMapThreaded(); //creates train Instances
			ib.createTestingDataMapThreaded(); //creates test Instances (if present)
			ib.killThreads();
		} catch (Exception e) {
			System.out.println("Failed to prepare instances");
			e.printStackTrace();
		}

	}
	
	/**
	 * Calculates and stores the infoGain for future use
	 */
	public void calcInfoGain(){
		try {
			ib.calculateInfoGain(); //delegate to underlying Instances Builder
		} catch (Exception e) {
			LOG.error("Failed to calculate infoGain",e);
		} 
	}
	
	/**
	 * Applies infoGain to the training and testing instances
	 * @param n the number of features/attributes to keep
	 */
	public void applyInfoGain(int n){
		try {
			ib.applyInfoGain(n);
		} catch (Exception e) {
			System.out.println("Failed to apply infoGain");
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform the actual analysis
	 */
	public void run(){
		
		//switch based on the enum
		switch (selected) {
	
		//do a cross val
		case CROSS_VALIDATION:
			experimentResults = analysisDriver.runCrossValidation(ib.getTrainingDataMap(), numFolds, 0);
			break;

		// do a train/test
		case TRAIN_TEST_UNKNOWN:
		    experimentResults = analysisDriver.classify(ib.getTrainingDataMap(), ib.getTestDataMap(), ib.getProblemSet().getAllTestDocs());
			break;

		//do a train/test where we know the answer and just want statistics
		case TRAIN_TEST_KNOWN:
			ib.getProblemSet().removeAuthor("_Unknown_");
			try {
				DataMap train = ib.getTrainingDataMap();
				DataMap test = ib.getTestDataMap();
				experimentResults = analysisDriver.getTrainTestEval(train,test);
			} catch (Exception e) {
				LOG.error("Failed to build trainTest Evaluation",e);
			}
			break;
		
		//should not occur
		default:
			System.out.println("Unreachable. Something went wrong somewhere.");
			break;
		}
	}
	
	/**
	 * Runs verification with the extracted instances
	 * right now both verifiers only need a single double arg, so this parameter works out.
	 * Might need to adjust this to add more verifiers, however.
	 */
	//TODO this'll need to be redone to not use Weka classes
	/*
	public void verify(double arg){
		if (verifierName.equalsIgnoreCase("ThresholdVerifier")){
			List<String> authors = new ArrayList<String>();
			for (String s : ib.getProblemSet().getAuthors()){
				authors.add(s);
			}
			Instances tests = WekaAnalyzer.instancesFromDataMap(ib.getTestDataMap());
			for (int i = 0; i < tests.numInstances(); i++){
				Instance inst = tests.instance(i);
				verifier = new ThresholdVerifier(analysisDriver.getClassifier(),inst,arg,authors);
			}
		} else if (verifierName.equalsIgnoreCase("Distractorless")) {
			verifier = new DistractorlessVerifier(WekaAnalyzer.instancesFromDataMap(ib.getTrainingDataMap()),WekaAnalyzer.instancesFromDataMap(ib.getTestDataMap()),true,arg);
		}
		verifier.verify();
	}
	*/
	
	///////////////////////////////// Setters/Getters
	
	/**
	 * Spits back the verification result string
	 * @return
	 */
	public String getVerificationResultString(){
		return verifier.getResultString();
	}
	
	
	/**
	 * @param useCache boolean value. Whether or not to use the cache for feature extraction.
	 */
	public void setUseCache(boolean useCache) {
		ib.setUseCache(useCache);
	}
	
	public void setProblemSet(ProblemSet probSet){
		ib.setProblemSet(probSet);
	}
	
	/**
	 * Sets the training Instances object
	 * @param insts the Instances object to use as training data
	 */
	public void setTrainingDataMap(DataMap train){
		ib.setTrainingDataMap(train);
	}
	
	/**
	 * Sets the testing Instances object
	 * @param insts the Instances object to use as testing data
	 */
	public void setTestingDataMap(DataMap test){
		ib.setTestingDataMap(test);
	}
	
	/**
	 * Sets the type of experiment to run
	 * @param type enum value of either CROSS_VALIDATION, TRAIN_TEST_UNKNOWN, or TRAIN_TEST_KNOWN
	 */
	public void setExperimentType(analysisType type){
		selected = type;
	}
	
	/**
	 * Change the number of folds to use in cross validation
	 * @param n number of folds to use from now on
	 */
	public void setNumFolds(int n){
		numFolds = n;
	}
	
	/**
	 * Sets the number of calculation threads to use
	 * @param nt the number of calculation threads to use
	 */
	public void setNumThreads(int nt){
		ib.setNumThreads(nt);
	}
	
	public boolean isUsingCache() {
		return ib.isUsingCache();
	}
	
	public Analyzer getUnderlyingAnalyzer(){
		return analysisDriver;
	}
	
	/**
	 * @return the Instances object describing the training documents
	 */
	public DataMap getTrainingDataMap(){
		return ib.getTrainingDataMap();
	}
	
	/**
	 * @return the Instances object describing the test documents
	 */
	public DataMap getTestingDataMap(){
		return ib.getTestDataMap();
	}
	
	/**
	 * @return the infoGain data (not in human readable form lists indices and usefulness)
	 */
	public double[][] getInfoGain(){
		return ib.getInfoGain();
	}
	
	/**
	 * @return the problem set being evaluated
	 */
	public ProblemSet getProblemSet(){
		return ib.getProblemSet();
	}
	
	/**
	 * @return the InstancesBuilder which is responsible for the feature extraction
	 */
	public LocalParallelFeatureExtractionAPI getUnderlyingInstancesBuilder(){
		return ib;
	}
	
	/**
	 * Returns a string of features, in order of most to least useful, with their infogain values<br>
	 * @param showZeroes whether or not to show features that have a 0 as their infoGain value
	 * @return the string representing the infoGain
	 */
	public String getReadableInfoGain(boolean showZeroes){
		
		//initialize the string and infoGain
		String infoString = ">-----InfoGain information: \n\n";
		DataMap trainingDataMap = ib.getTrainingDataMap();
		double[][] infoGain = ib.getInfoGain();
		
		for (int i = 0; i<infoGain.length; i++){
			if (!showZeroes && (infoGain[i][0]==0))
				break;
			
			//match the index to the name and add it to the string
			infoString+=String.format("> %-50s   %f\n",
					trainingDataMap.getFeatures().get((int)infoGain[i][1]),
					infoGain[i][0]);
		}
		
		//return the result
		return infoString;
	}
	
	/**
	 * @return the evaluation object containing the classification data
	 */
	public ExperimentResults getResults(){
		return experimentResults;
	}
	
	/**
	 * @return String containing accuracy, metrics, and confusion matrix from the evaluation
	 */
    public String getStatString() {
        ExperimentResults eval = getResults();
        String resultsString = "";
        //change the experiment header
        if (selected.equals(analysisType.CROSS_VALIDATION))
            resultsString+="[[Showing results for Cross Validation Experiment]]\n";
        else if (selected.equals(analysisType.TRAIN_TEST_KNOWN)) {
            resultsString+="[[Showing results for a Train-Test with Known Authors Experiment]]\n";
        } else if (selected.equals(analysisType.TRAIN_TEST_UNKNOWN)){
            resultsString+="[[Showing results for a Train-Test with Unknown Authors Experiment]]\n";
            resultsString+=eval.getAllDocumentResults(false);
            return resultsString; //return this one early as we don't want to add the stat string to it since it'd be misleading
        } else {
            resultsString+="[[Showing results for an unidentifiable experiment... how did you get here?]]\n";
        }
            
        resultsString += eval.getStatisticsString() + "\n";
        if (selected.equals(analysisType.CROSS_VALIDATION))
            resultsString += eval.getConfusionMatrixString();
        else
            resultsString += eval.getAllDocumentResults(true) + "\n";
        return resultsString;
	}

    
	/**
	 * @return The accuracy of the given test in percentage format
	 */
	public String getClassificationAccuracy(){
		String results = "";
		ExperimentResults eval = getResults();
		results+= eval.getTruePositiveRate();
		
		return results;
	}
	
	public CumulativeFeatureDriver getCFD(){
		return ib.getCFD();
	}
	
	///////////////////////////////// Main method for testing purposes
	//FIXME
	/*
	 * So right now there's 1 mild bug
	 * 
	 * 1: Crossvalidation--weka seems to shuffle instances when cross-validating. 
	 *     This causes document titles to be improperly tracked throughout the whole process.
	 *     
	 *     Fix idea--do cross validation "manually"--randomly determine which docs to use as testing ourselves,
	 *     keeping track of it as we go. Use the source instances to break apart and create new train test and accumulate
	 *     results manually.
	 *     Note: this is purely incorrect in terms of labels. The overall statistics produced is correct, just the wrong doc titles are being assigned.
	 * 
	 */
	public static void main(String[] args){
	    
	    FullAPI test = null;
	    
	    ProblemSet ps = new ProblemSet();
	    File parent = new File("/Users/tdutko200/git/jstylo/jsan_resources/corpora/drexel_1");
        try {
            for (File author : parent.listFiles()) {
                if (!author.getName().equalsIgnoreCase(".DS_Store")) {
                    for (File document : author.listFiles()) {
                        if (!document.getName().equalsIgnoreCase(".DS_Store")) {
                            Document doc = new StringDocument(toDeleteGetStringFromFile(document), author.getName(),document.getName());
                            doc.load();
                            ps.addTrainDoc(author.getName(), doc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Womp womp.",e);
            System.exit(1);
        }
	    
        try {
            test = new FullAPI.Builder()
                    .cfdPath("jsan_resources/feature_sets/writeprints_feature_set_limited.xml")
                    .ps(ps)
                    .setAnalyzer(new WekaAnalyzer())
                    .numThreads(1).analysisType(analysisType.CROSS_VALIDATION).useCache(false).chunkDocs(false)
                    .loadDocContents(true)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to intialize API, exiting...");
            System.exit(1);
        }

		test.prepareInstances();
		test.calcInfoGain();
		//test.applyInfoGain(5);
		test.run();
		System.out.println(test.getStatString());
		System.out.println(test.getReadableInfoGain(false));
		System.out.println(test.getResults().toJson().toString());
		//System.out.println(test.getClassificationAccuracy());
		//System.out.println(test.getStatString());
	}
	
	private static String toDeleteGetStringFromFile(File f) throws Exception{
	    BufferedReader br = new BufferedReader(new FileReader(f));
	    String results = "";
	    while (br.ready())
	        results+=br.readLine()+"\n";
	    br.close();
	    return results;
	}
}

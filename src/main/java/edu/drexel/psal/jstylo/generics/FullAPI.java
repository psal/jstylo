package edu.drexel.psal.jstylo.generics;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgaap.generics.Document;
import com.jgaap.generics.EventSet;

import edu.drexel.psal.jstylo.featureProcessing.Chunker;
import edu.drexel.psal.jstylo.featureProcessing.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.featureProcessing.LocalParallelFeatureExtractionAPI;
import edu.drexel.psal.jstylo.featureProcessing.ProblemSet;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import edu.drexel.psal.jstylo.machineLearning.Verifier;
import edu.drexel.psal.jstylo.machineLearning.weka.InfoGain;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;
import edu.drexel.psal.jstylo.verifiers.DistractorlessVerifier;

/**
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
		private boolean applyInfoGain = false;
		private int featuresToKeep = 500;
		
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
		
		public Builder setApplyInfoGain(boolean apply){
		    applyInfoGain = apply;
		    return this;
		}
		
		public Builder setFeaturesToKeep(int numFeatures){
		    featuresToKeep = numFeatures;
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
	CumulativeFeatureDriver cfd;
	double[][] featureWeights;
	int numFeaturesToKeep;
	boolean applyInfoGain;
	DataMap training;
	DataMap testing;
	
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
		
		if (b.cfdXMLPath==null) {
		    cfd = b.cfd;
        } else {
            try {
                cfd = new CumulativeFeatureDriver(b.cfdXMLPath);
            } catch (Exception e) {
                LOG.error("Failed to build cfd", e);
            }
        }
		
		ib.setUseCache(b.useCache);
		ib.setLoadDocContents(b.loadDocContents);
		ib.setChunkDocs(b.chunkDocs);
		verifierName = b.verifierName;
		selected = b.type;
		numFolds = b.numFolds;
		analysisDriver = b.analyzer;
		numFeaturesToKeep = b.featuresToKeep;
		applyInfoGain = b.applyInfoGain;
	}
	
	///////////////////////////////// Methods
	
	/**
	 * Prepares the instances objects (stored within the InstancesBuilder)
	 */
	public void prepareInstances() {

		try {
			if (ib.isUsingCache())
				ib.validateCFDCache(cfd);
			if (ib.isChunkingDocs())
			    Chunker.chunkAllTrainDocs(ib.getProblemSet());
			List<List<EventSet>> eventList = ib.extractEventsThreaded(cfd); //extracts events from documents
			List<EventSet> relevantEvents = ib.getRelevantEvents(eventList,cfd); //creates the List<EventSet> to pay attention to
			List<String> features = ib.getFeatureList(eventList,relevantEvents, cfd); //creates the attribute list to base the Instances on
			training = ib.createTrainingDataMapThreaded(eventList,relevantEvents,features,cfd); //creates train Instances
			testing = ib.createTestingDataMapThreaded(eventList,relevantEvents,features,cfd); //creates test Instances (if present)
			if (applyInfoGain){
			    applyInfoGain(training);
			    applyInfoGain(testing);
			}
			    
			ib.killThreads();
		} catch (Exception e) {
			LOG.info("Failed to prepare instances");
			e.printStackTrace();
		}

	}
	
	/**
	 * Calculates and stores the infoGain for future use
	 */
	public void calcInfoGain(){
		try {
			featureWeights = InfoGain.calcInfoGain(training); //delegate to underlying Instances Builder
		} catch (Exception e) {
			LOG.error("Failed to calculate infoGain",e);
		} 
	}
	
	/**
	 * Applies infoGain to the training and testing instances
	 * @param n the number of features/attributes to keep
	 */
	private void applyInfoGain(DataMap data){
	    if (featureWeights == null)
	        calcInfoGain();
		try {
			InfoGain.applyInfoGain(featureWeights,data,numFeaturesToKeep);
		} catch (Exception e) {
			LOG.info("Failed to apply infoGain");
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
			experimentResults = analysisDriver.runCrossValidation(training, numFolds, 0);
			break;

		// do a train/test
		case TRAIN_TEST_UNKNOWN:
		    experimentResults = analysisDriver.classifyWithUnknownAuthors(training, testing, ib.getProblemSet().getAllTestDocs());
			break;

		//do a train/test where we know the answer and just want statistics
		case TRAIN_TEST_KNOWN:
			ib.getProblemSet().removeAuthor("_Unknown_");
			try {
				experimentResults = analysisDriver.classifyWithKnownAuthors(training,testing);
			} catch (Exception e) {
				LOG.error("Failed to build trainTest Evaluation",e);
			}
			break;
		
		//should not occur
		default:
			LOG.info("Unreachable. Something went wrong somewhere.");
			break;
		}
	}
	
	/**
	 * Runs verification with the extracted instances
	 * right now both verifiers only need a single double arg, so this parameter works out.
	 * Might need to adjust this to add more verifiers, however.
	 */
	public void verify(double arg){
		if (verifierName.equalsIgnoreCase("Distractorless")) {
			verifier = new DistractorlessVerifier(training,testing,true,arg);
		} else {
		    LOG.error("Specified Verifier "+verifierName+" is not yet supported. Skipping verification...");
		    return;
		}
		verifier.verify();
	}
	
	
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
		training = train;
	}
	
	/**
	 * Sets the testing Instances object
	 * @param insts the Instances object to use as testing data
	 */
	public void setTestingDataMap(DataMap test){
		testing = test;
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
		return training;
	}
	
	/**
	 * @return the Instances object describing the test documents
	 */
	public DataMap getTestingDataMap(){
		return testing;
	}
	
	/**
	 * @return the infoGain data (not in human readable form lists indices and usefulness)
	 * @throws Exception 
	 */
	public double[][] getInfoGain(DataMap data) throws Exception{
		return InfoGain.calcInfoGain(data);
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
		DataMap trainingDataMap = training;
		double[][] infoGain = featureWeights;
		
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
            resultsString+=eval.getSimpleResults();
            return resultsString; //return this one early as we don't want to add the stat string to it since it'd be misleading
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
		return cfd;
	}
	
	public static void main(String[] args){
	    /*
	    ProblemSet ps = new ProblemSet("./jsan_resources/problem_sets/politicians.xml");
	    
	    ExperimentResults cumulativeResults = new ExperimentResults();
	    int count = 1;
	    for (Document toTest : ps.getAllTrainDocs()){
	        ProblemSet ps2 = new ProblemSet(ps);
	        ps2.removeTrainDocAt(toTest.getAuthor(), toTest);
	        ps2.addTestDoc(toTest.getAuthor(), toTest);
	        
	        FullAPI test = null;
	        
	        try {
	            test = new FullAPI.Builder()
	                    .cfdPath("jsan_resources/feature_sets/politics.xml")
	                    .ps(ps2)
	                    .setAnalyzer(new WekaAnalyzer())
	                    .numThreads(4).analysisType(analysisType.TRAIN_TEST_KNOWN)
	                    .useCache(false).chunkDocs(false)
	                    .build();
	        } catch (Exception e) {
	            e.printStackTrace();
	            LOG.error("Failed to intialize API, exiting...",e);
	        }
	        
	        test.prepareInstances();
	        test.run();
	        for (DocResult dr : test.getResults().getExperimentContents()){
	            cumulativeResults.addDocResult(dr);
	        }
	        LOG.info("Completed "+count+" of "+ps.getAllTrainDocs().size());
	        count++;
	    }
	    
	    LOG.info(cumulativeResults.getStatisticsString()+"\n"+cumulativeResults.getAllDocumentResults(true)+"\n"+cumulativeResults.getConfusionMatrixString());
	    */
	    
	    ProblemSet ps = new ProblemSet("jsan_resources/problem_sets/trumpMiller.xml");
	    
	    FullAPI test = null;
	    
        try {
            test = new FullAPI.Builder()
                    .cfdPath("jsan_resources/feature_sets/writeprints_limited_norm_revised.xml")
                    .ps(ps)
                    .setAnalyzer(new WekaAnalyzer())
                    .numThreads(4).analysisType(analysisType.TRAIN_TEST_UNKNOWN).useCache(false).chunkDocs(false)
                    .loadDocContents(false)
                    .numFolds(3)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Failed to intialize API, exiting...",e);
        }

        test.prepareInstances();
        Verifier v = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false);
        v.verify();
        LOG.info("Default verification\n"+v.getResultString());
        
        Verifier v3 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false, 1.0);
        v3.verify();
        LOG.info("100% known verification\n"+v3.getResultString());
        
        Verifier v2 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false, .86);
        v2.verify();
        LOG.info("85% known verification\n"+v2.getResultString());
        
        Verifier v4 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false,.72);
        v4.verify();
        LOG.info("50% known verification\n"+v4.getResultString());
        
        Verifier v7 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false,.58);
        v7.verify();
        LOG.info("50% known verification\n"+v7.getResultString());
        
        Verifier v6 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false,.43);
        v6.verify();
        LOG.info("50% known verification\n"+v6.getResultString());
        
        Verifier v5 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false,.29);
        v5.verify();
        LOG.info("50% known verification\n"+v5.getResultString());
        
        Verifier v8 = new DistractorlessVerifier(test.getTrainingDataMap(),test.getTestingDataMap(),false,.15);
        v8.verify();
        LOG.info("50% known verification\n"+v8.getResultString());
		
		//test.calcInfoGain();
		//test.applyInfoGain(5);
		//test.run();
		//LOG.info(test.getUnderlyingAnalyzer().getExperimentMetrics());
		//LOG.info(test.getStatString());
		//LOG.info(test.getReadableInfoGain(false));
		//LOG.info(test.getResults().toJson().toString());
		//LOG.info("Count for "+test.getTestingDataMap().getFeatures().get(0)+
		//        " is "+test.getTestingDataMap().getDataMap().get("a").get("a_07.txt").getFeatureCountAtIndex(0));
		//LOG.info(test.getClassificationAccuracy());
		//LOG.info(test.getStatString());
	}
	
}

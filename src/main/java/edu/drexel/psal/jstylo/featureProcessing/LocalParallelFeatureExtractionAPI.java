package edu.drexel.psal.jstylo.featureProcessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventSet;

import edu.drexel.psal.JSANConstants;
import edu.drexel.psal.jstylo.featureProcessing.CumulativeFeatureDriver.FeatureSetElement;
import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocumentData;
import edu.drexel.psal.jstylo.generics.Preferences;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import edu.drexel.psal.jstylo.machineLearning.weka.InfoGain;

/**
 * An API for the feature extraction process. Designed for running on a single machine while utilizing multi-threading.
 * @author Travis Dutko
 */
public class LocalParallelFeatureExtractionAPI extends FeatureExtractionAPI {

    private static final Logger LOG = LoggerFactory.getLogger(LocalParallelFeatureExtractionAPI.class);
    
	// These vars should be initialized in the constructor and stay the same
	// throughout the entire process
	private Preferences preferences;
	private boolean isCacheValid;
	
	// persistant data stored as we create it
	private ProblemSet ps;	//the documents
	private CumulativeFeatureDriver cfd;	//the driver used to extract features
	private List<List<EventSet>> eventList;	//the events/sets created from the extracted features
	private List<EventSet> relevantEvents;	//the events/sets to pay attention to
	private List<String> features;
	
	//ThreadArrays so that we can stop them if the user cancels something mid process
	FeatureExtractionThread[] featThreads;
	CreateTrainDataMapThread[] trainThreads;
	CreateTestDataMapThread[] testThreads;
	
	// Relevant data to spit out at the end
	private DataMap trainingDataMap; //training doc Instances
	private DataMap testingDataMap;	//testDoc Instances
	private double[][] infoGain;	//infoGain scores for all features

	/**
	 * Builder for the InstancesBuilder class.<br>
	 * 
	 * You must specifify at least one value from each of the following parameter pairs:<br>
	 * psPath or ps<br>
	 * cfdPath or cfd<br>
	 * 
	 * All other parameters have the following default values:<br>
	 * numThreads: 4<br>
	 * useCache = false<br>
	 * loadDocContents = false<br>
	 */
	public static class Builder{
		private String psPath;
		private String cfdPath;
		private ProblemSet ps;
		private CumulativeFeatureDriver cfd;
		private boolean useCache = false;
		private boolean loadDocContents = false;
		private int numThreads = 4;
		private Preferences p = null;
		
		public Builder psPath(String psp){
			psPath = psp;
			return this;
		}
		
		public Builder cfdPath(String cfdp){
			cfdPath = cfdp;
			return this;
		}
		
		public Builder ps(ProblemSet pset){
			ps = pset;
			return this;
		}
		
		public Builder cfd(CumulativeFeatureDriver cfdr){
			cfd = cfdr;
			return this;
		}
		
		public Builder useCache(boolean uc){
		    useCache=uc;
		    return this;
		}
		
		public Builder numThreads(int nt){
		    numThreads = nt;
			return this;
		}
		
		public Builder preferences(Preferences pref){
			p = pref;
			return this;
		}
		
		public LocalParallelFeatureExtractionAPI build(){
			return new LocalParallelFeatureExtractionAPI(this);
		}
		
	}
	//////////////////////////////////////////// Constructors
	
	/**
	 * Empty constructor. For use if you want to build it as you go, rather then
	 * all at once<br>
	 * Ensure not to try to do anything fancy with it until all required
	 * information is set
	 */
	public LocalParallelFeatureExtractionAPI() {}

	/**
	 * Build straight from a preferences file. Not recommended, but possible if you know what you're doing.
	 * @param p
	 */
	public LocalParallelFeatureExtractionAPI(Preferences p){
		preferences = p;
	}
	
	/**
	 * Builder constructor. Preferred method of constructing the object.
	 * 
	 * @param b the builder to use
	 */
	public LocalParallelFeatureExtractionAPI(Builder b){
		if (b.psPath==null)
			ps = b.ps;
		else
			ps = new ProblemSet(b.psPath,false);
		
		if (b.cfdPath==null)
			cfd = b.cfd;
		else {
			try {
				cfd = new CumulativeFeatureDriver(b.cfdPath);
			} catch (Exception e) {
				LOG.error("Failed to build cfd",e);
			}
		}
		
		if (b.p == null){
			preferences = Preferences.buildDefaultPreferences();
		} else {
			preferences = b.p;
		}
		
		if (b.loadDocContents){
		    preferences.setPreference("loadDocContents", "1");
		} else {
		    preferences.setPreference("loadDocContents", "0");
		}
		
        preferences.setPreference("numCalcThreads", ""+b.numThreads);
		
        if (b.useCache) {
            preferences.setPreference("useCache", "1");
            isCacheValid = validateCFDCache();
        } else {
            preferences.setPreference("useCache", "0");
            isCacheValid = false;
        }
	}
	
	/**
	 * Copy constructor
	 * @param original the original API object to copy over
	 */
	public LocalParallelFeatureExtractionAPI(LocalParallelFeatureExtractionAPI original){
		ps = original.getProblemSet();
		cfd = original.getCFD();
		preferences = original.getPreferences();
	}

	//////////////////////////////////////////// Methods
	
	/**
	 * Extracts the List\<EventSet\> from each document using a user-defined number of threads.
	 * Culls the eventSets as well.
	 * @throws Exception
	 */
	public void extractEventsThreaded() throws Exception {

		//pull in documents and find out how many there are
		List<Document> knownDocs = ps.getAllTrainDocs();
		int knownDocsSize = knownDocs.size();

		// initalize empty List<List<EventSet>>
		eventList = new ArrayList<List<EventSet>>(knownDocsSize);
		
		// if the num of threads is bigger then the number of docs, set it to
		// the max number of docs (extract each document's features in its own
		// thread
		int numThreads = getNumThreads();
		int threadsToUse = numThreads;
		
		//if we have more threads than documents, that's a bit silly.
		if (numThreads > knownDocsSize) {
			threadsToUse = knownDocsSize;
		}
		
		//if some documents are leftover after divvying them up, increment the div
		int div = knownDocsSize / threadsToUse;
		if (div % threadsToUse != 0)
			div++;
		
		LOG.info("Beginning Feature Extraction");
		
		//Parallelized feature extraction
		featThreads = new FeatureExtractionThread[threadsToUse];
		for (int thread = 0; thread < threadsToUse; thread++) //create the threads
			featThreads[thread] = new FeatureExtractionThread(div, thread,
					knownDocsSize, knownDocs, new CumulativeFeatureDriver(cfd));
		for (int thread = 0; thread < threadsToUse; thread++) //start them
			featThreads[thread].start();
		for (int thread = 0; thread < threadsToUse; thread++) //join them
			featThreads[thread].join();
		for (int thread = 0; thread < threadsToUse; thread++) //combine List<List<EventSet>>
			eventList.addAll(featThreads[thread].list);
		for (int thread = 0; thread < threadsToUse; thread++) //destroy threads
			featThreads[thread] = null;
		
		featThreads = null;
		
		//cull the List<List<EventSet>> before returning
		List<List<EventSet>> temp = cull(eventList, cfd);
		
		LOG.info("Feature Extraction complete");
		
		//return it now
		eventList = temp;
	}

	/**
	 * Initializes and stores the list of relevantEvents created from the culled List\<List\<EventSet\>\>
	 * @throws Exception
	 */
	public void initializeRelevantEvents() throws Exception {
		relevantEvents = getRelevantEvents(eventList, cfd);
	}

	/**
	 * Extracts the feature labels being used for the processing
	 * @throws Exception
	 */
	public void initializeFeatureSet() throws Exception {
	    features = getFeatureList(eventList,relevantEvents,cfd);
	}

	/**
	 * Threaded creation of training datamap from gathered data
	 * @throws Exception
	 */
	public void createTrainingDataMapThreaded() throws Exception {

	    trainingDataMap = new DataMap("Training",features);
	    
		//initialize/fetch data
		int numThreads = getNumThreads();
		int threadsToUse = numThreads;
		int numInstances = eventList.size();
		
		//if there are more threads than instances, that's just silly
		if (numThreads > numInstances) {
			threadsToUse = numInstances;
		}

		//initialize the div and make sure it captures everything
		int div = numInstances / threadsToUse;
		if (div % threadsToUse != 0)
			div++;

		//pre-process the map to add all authors
		for (List<EventSet> docEvents : eventList){
		    EventSet metadata = docEvents.get(docEvents.size()-1);
		    String author = metadata.eventAt(0).getEvent();
		    trainingDataMap.initAuthor(author);
		}
		
		//Parallelized magic
		trainThreads = new CreateTrainDataMapThread[threadsToUse];
		for (int thread = 0; thread < threadsToUse; thread++)
			trainThreads[thread] = new CreateTrainDataMapThread(div,thread,numInstances,new CumulativeFeatureDriver(cfd));
		for (int thread = 0; thread < threadsToUse; thread++)
			trainThreads[thread].start();
		for (int thread = 0; thread < threadsToUse; thread++)
			trainThreads[thread].join();
		for (int thread = 0; thread < threadsToUse; thread++)
			trainThreads[thread] = null;
		trainThreads = null;
		
	}

	/**
	 * Creates Test datamap from all of the information gathered (if there are any)
	 * @throws Exception
	 */
	public void createTestingDataMapThreaded() throws Exception {
		
		//if there are no test instances, set the instance object to null and move on with our lives
		if (ps.getAllTestDocs().size()==0){
			testingDataMap=null;
		} else { //otherwise go through the whole process
			testingDataMap = new DataMap("testing data",features);
			//create/fetch data
			int numThreads = getNumThreads();
			int threadsToUse = numThreads;
			int numInstances = ps.getAllTestDocs().size();
		
		    //pre-process the map to add all authors
	        for (List<EventSet> docEvents : eventList){
	            EventSet metadata = docEvents.get(docEvents.size()-1);
	            String author = metadata.eventAt(0).getEvent();
	            testingDataMap.initAuthor(author);
	        }
			
			//make sure number of threads isn't silly
			if (numThreads > numInstances) {
				threadsToUse = numInstances;
			}

			//ensure the docs are divided correctly
			int div = numInstances / threadsToUse;
			if (div % threadsToUse != 0)
				div++;

			//Perform some parallelization magic
			testThreads = new CreateTestDataMapThread[threadsToUse];
			for (int thread = 0; thread < threadsToUse; thread++)
				testThreads[thread] = new CreateTestDataMapThread(div,thread,numInstances, new CumulativeFeatureDriver(cfd));
			for (int thread = 0; thread < threadsToUse; thread++)
				testThreads[thread].start();
			for (int thread = 0; thread < threadsToUse; thread++)
				testThreads[thread].join();
			for (int thread = 0; thread < threadsToUse; thread++)
				testThreads[thread] = null;
			testThreads = null;
			
		}
	}

	//////////////////////////////////////////// InfoGain related things

	/**
	 * Applies the infoGain information to the training and (if present) test
	 * Instances. Overwrites our infoGain with the the revised version
	 * @throws Exception
	 */
	public void applyInfoGain(int n) throws Exception {
		setInfoGain(InfoGain.applyInfoGain(getInfoGain(), trainingDataMap, n));
		if (testingDataMap != null){ // Apply infoGain to test set if we have one
			InfoGain.applyInfoGain(getInfoGain(), testingDataMap, n);
		}
	}

	/**
	 * Calculates infoGain across the trainingInstances
	 * @return a double[][] containing an entry for each feature denoting its index and how useful it is. Sorted via usefulness.
	 * @throws Exception
	 */
	public double[][] calculateInfoGain() throws Exception{
		setInfoGain(InfoGain.calcInfoGain(trainingDataMap));
		return getInfoGain();
	}
	
	//////////////////////////////////////////// Setters/Getters
	
	/**
	 * @param probSet sets the problem set to the provided pset
	 */
	public void setProblemSet(ProblemSet probSet){
		ps = null;
		ps = probSet;
	}
	
	/**
	 * @param cumulativeFeatureDriver the cfd to use
	 */
	public void setCumulativeFeatureDriver(CumulativeFeatureDriver cumulativeFeatureDriver){
		cfd = cumulativeFeatureDriver;
	}
	
	/**
	 * @return returns the current cfd
	 */
	public CumulativeFeatureDriver getCFD(){
		return cfd;
	}
	
	/**
	 * @return Returns the infoGain value and stores it locally in case we decide
	 *         to apply it
	 */
	public double[][] getInfoGain() {
		return infoGain;
	}

	/**
	 * @param doubles the double array created by the infoGain
	 */
	public void setInfoGain(double[][] doubles){
		infoGain = doubles;
	}
	
	/**
	 * @param ldc whether or not we should load doc contents
	 */
	public void setLoadDocContents(boolean ldc){
		if (ldc)
			preferences.setPreference("loadDocContents","1");
		else
			preferences.setPreference("loadDocContents","0");
	}
	
	/**
	 * @return Returns the problem set used by the InstancesBuilder
	 */
	public ProblemSet getProblemSet(){
		return ps;
	}
	
	/**
	 * A niche method for when you already have a training DataMap object
	 * @param ti training DataMap object
	 */
	public void setTrainingDataMap(DataMap tdm) {
		trainingDataMap = tdm;
	}
	
	/**
	 * Updates the API's preferences
	 * @param pref
	 */
	public void setPreferences(Preferences pref){
		preferences = pref;
	}
	
	/**
	 * A niche method for when you already have a testing DataMap object
	 * @param ti testing DataMap object
	 */
	public void setTestingDataMap(DataMap tdm){
		testingDataMap = tdm;
	}
	
	/**
	 * @return The DataMap object representing the training documents
	 */
	public DataMap getTrainingDataMap() {
		return trainingDataMap;
	}

	/**
	 * @return The DataMap object representing the test document(s)
	 */
	public DataMap getTestDataMap() {
		return testingDataMap;
	}

	/**
	 * Sets the number of calculation threads to use for feature extraction.
	 * @param nct number of calculation threads to use.
	 */
	public void setNumThreads(int nct) {
		preferences.setPreference("numCalcThreads", ""+nct);
	}
	
	/**
	 * @return the number of calculation threads we're using
	 */
	public int getNumThreads() {
		String s = preferences.getPreference("numCalcThreads");
		return Integer.parseInt(s);
	}
	
	/**
	 * @return whether or not we are pre-loading doc contents
	 */
	public boolean loadingDocContents(){
		return preferences.getBoolPreference("loadDocContents");
	}
	
	/**
	 * Gets the preference object
	 * @return
	 */
	public Preferences getPreferences() {
		return preferences;
	}
	
	/**
	 * @return the list of training documents
	 */
	public List<Document> getTrainDocs(){
		return ps.getAllTrainDocs();
	}
	
	/**
	 * @return the list of testing documents
	 */
	public List<Document> getTestDocs(){
		return ps.getAllTestDocs();
	}

	//////////////////////////////////////////// Utilities
	
	/**
	 * Sets classification relevant data to null
	 */
	public void reset() {
		ps = null;
		cfd = null;
		infoGain = null;
		testingDataMap = null;
		trainingDataMap = null;
		killThreads();
	}

	@SuppressWarnings("unused")
    public void clean() {

        for (EventSet es : relevantEvents) {
            for (Event ev : es) {
                ev = null;
            }
            es = null;
        }
        relevantEvents.clear();
        relevantEvents = null;

        for (List<EventSet> les : eventList) {
            for (EventSet es : les) {
                for (Event e : es) {
                    e = null;
                }
                es = null;
            }
            les = null;
        }
        eventList.clear();
        eventList = null;

        cfd.clean();
        cfd = null;

        System.gc();
    }
	
	/**
	 * For use when stopping analysis mid-way through it. Kills any processing threads
	 */
	@SuppressWarnings("deprecation")
	public void killThreads() {
		
		//feature extraction threads
		if (featThreads!=null){
			for (int i=0; i<featThreads.length; i++){
				featThreads[i].stop();
			}
			for (int i=0; i<featThreads.length; i++){
				featThreads[i] = null;
			}
			featThreads=null;
		}
		
		//training instances threads
		if (trainThreads!=null){
			for (int i=0; i<trainThreads.length; i++){
				trainThreads[i].stop();
			}
			for (int i=0; i<trainThreads.length; i++){
				trainThreads[i] = null;
			}
			trainThreads=null;
		}
		
		//testing instances threads
		if (testThreads!=null){
			for (int i=0; i<testThreads.length; i++){
				testThreads[i].stop();
			}
			for (int i=0; i<testThreads.length; i++){
				testThreads[i] = null;
			}
			testThreads=null;
		}
	}

	//////////////////////////////////////////// Thread Definitions
	
	/**
	 * A thread used to parallelize the population of the TestingDataMap from a set of documents
	 * @param d The "div" or divide--how many documents each thread processes at most
	 * @param t The threadId. Keeps track of which thread is doing which div of documents
	 * @param n the total number of instances (used to put a cap on the last div so it doesn't try to process docs which don't exist
	 * @param cd A copy of the cfd to assess features with
	 * @author Travis Dutko
	 */
	public class CreateTestDataMapThread extends Thread {
		
		int div; //the number of instances to be created per thread
		int threadId; //the div this thread is dealing with
		int numInstances; //the total number of instances to be created
		CumulativeFeatureDriver cfd; //the cfd used to assess features
		
		//Constructor
		public CreateTestDataMapThread(int d, int t, int n, CumulativeFeatureDriver cd){
			cfd=cd;
			div = d;
			threadId = t;
			numInstances = n;
		}
		
		//Run method
		@Override
		public void run() {
			//for all docs in this div
			for (int i = div * threadId; i < Math.min(numInstances, div
					* (threadId + 1)); i++)
				try {
					//grab the document
					Document document = ps.getAllTestDocs().get(i);
					//extract its event sets
					List<EventSet> events = extractEventSets(document, cfd,loadingDocContents(),isCacheValid);
					//cull the events/eventSets with respect to training events/sets
					events = cullWithRespectToTraining(relevantEvents, events, cfd);
					
					//build the doc data
	                String author = events.get(events.size()-1).eventAt(0).getEvent();
	                String title = events.get(events.size()-1).eventAt(1).getEvent();
	                ConcurrentHashMap<Integer,FeatureData> docdata = createDocMap(features, relevantEvents, cfd, events);

	                DocumentData doc = new DocumentData(getNormalizations(events.get(events.size()-1)), docdata);
					//normalize it
					normDocData(doc);
					
					//add it to datamap
					testingDataMap.addDocumentData(author, title, doc);
				} catch (Exception e) {
				    LOG.error("Error creating Test Document data for "+
				            ps.getAllTestDocs().get(i).getFilePath()+" author: "+ps.getAllTestDocs().get(i).getAuthor(),e);
				}
		}
		
	}
	
	/**
	 * A thread used to parallelize the population of TrainingDataMap from a set of documents
	 * @param d The "div" or divide--how many documents each thread processes at most
	 * @param t The threadId. Keeps track of which thread is doing which div of documents
	 * @param n the total number of instances (used to put a cap on the last div so it doesn't try to process docs which don't exist
	 * @param cd A copy of the cfd to assess features with
	 * @author Travis Dutko
	 */
	public class CreateTrainDataMapThread extends Thread {
		
		int div; //the number of instances to be created per thread
		int threadId; //the div for this thread
		int numdocs; //the total number of docs to be processed
		CumulativeFeatureDriver cfd; //the cfd used to identify features/attributes
		
		//Constructor
		public CreateTrainDataMapThread(int d, int t, int n,CumulativeFeatureDriver cd){
			div = d;
			threadId = t;
			numdocs = n;
			cfd=cd;
		}
		
		//Run method
		@Override
		public void run() {
			//for all docs in this div
			for (int i = div * threadId; i < Math.min(numdocs, div
					* (threadId + 1)); i++)
				try {
					//grab the document
				    //Logger.logln("[THREAD-" + threadId + "] Processing document " + i);
					String author = eventList.get(i).get(eventList.get(i).size()-1).eventAt(0).getEvent();
				    String title = eventList.get(i).get(eventList.get(i).size()-1).eventAt(1).getEvent();
				    ConcurrentHashMap<Integer,FeatureData> docdata = createDocMap(features, relevantEvents, cfd, eventList.get(i));
					//normalize it
				    DocumentData doc = new DocumentData(getNormalizations(eventList.get(i).get(eventList.get(i).size()-1)),docdata);
					normDocData(doc);
					//add it to this div's list of completed instances
					trainingDataMap.addDocumentData(author, title, doc);
					
				} catch (Exception e) {
				    LOG.error("[THREAD-" + threadId + "] Error creating datamap " + i + " for document "+ps.getAllTrainDocs().get(i).getFilePath(),e);
				}
		}
		
	}
	/*
    * 2 : Sentences in document 
    * 3 : Words in document 
    * 4 : Characters in document 
    * 5 : Letters in document
    */
	/**
	 * A utility method for the conversion to datamap threads. This needs to be genericized at some point in the future
	 * @param es
	 * @return
	 */
	private static Map<String,Integer> getNormalizations(EventSet es){
	    Map<String,Integer> norms = new HashMap<String,Integer>();
	    norms.put(NormBaselineEnum.SENTENCES_IN_DOC.getTitle(), Integer.parseInt(es.eventAt(2).getEvent()));
	    norms.put(NormBaselineEnum.WORDS_IN_DOC.getTitle(), Integer.parseInt(es.eventAt(3).getEvent()));
	    norms.put(NormBaselineEnum.CHARS_IN_DOC.getTitle(), Integer.parseInt(es.eventAt(2).getEvent()));
	    norms.put(NormBaselineEnum.LETTERS_IN_DOC.getTitle(), Integer.parseInt(es.eventAt(3).getEvent()));
	    return norms;
	}
	
	/**
	 * A thread used to extract features from documents in parallel. 
	 * @param d the divide--how many documents each thread processes at most
	 * @param threadId keeps track of which thread is doing which div of documents
	 * @param knownDocs a list of documents to be divvied up and have its features extracted
	 * @param cd A copy of the cfd to assess features with
	 * @author Travis Dutko
	 */
	public class FeatureExtractionThread extends Thread {
		
		ArrayList<List<EventSet>> list = new ArrayList<List<EventSet>>(); //The list of event sets for this division of docs
		int div; //the number of docs to be processed per thread
		int threadId; //the div index of this thread
		int knownDocsSize; //the number of docs total
		List<Document> knownDocs; //the docs to be extracted
		CumulativeFeatureDriver cfd; //the cfd to do the extracting with
		
		/**
		 * @return The list of extracted event sets for this division of documents
		 */
		public ArrayList<List<EventSet>> getList() {
			return list;
		}

		//Constructor
		public FeatureExtractionThread(int div, int threadId,
				int knownDocsSize, List<Document> knownDocs,
				CumulativeFeatureDriver cfd) {
			
			this.div = div;
			this.threadId = threadId;
			this.knownDocsSize = knownDocsSize;
			this.knownDocs = knownDocs;
			try {
				this.cfd = new CumulativeFeatureDriver(cfd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Runnable Method
		@Override
		public void run() {
			//for all the documents in this div to a maximum of the number of docs
			for (int i = div * threadId; i < Math.min(knownDocsSize, div
					* (threadId + 1)); i++){
				try {
					//try to extract the events
				    LOG.info("[THREAD-" + threadId + "] Extracting features from document " + i);
					List<EventSet> extractedEvents = extractEventSets(ps.getAllTrainDocs().get(i),cfd,loadingDocContents(),isCacheValid);
					list.add(extractedEvents); //and add them to the list of list of eventsets
				} catch (Exception e) {
				    LOG.error("[THREAD-" + threadId + "] Error extracting features for document " + i + " from "+ps.getAllTrainDocs().get(i).getFilePath(),e);
				} 
			}
		}
	}


//////////////////////////////////////////// Caching methods
    
    public boolean isUsingCache() {
        return preferences.getBoolPreference("useCache");
    }

    public void setUseCache(boolean useCache) {
        if (useCache)
            preferences.setPreference("useCache", "1");
        else
            preferences.setPreference("useCache", "0");
    }
    
    /**
     * Determines if we can load cached features. Cullers are not taken into account, since the
     * features are cached and cached features are loaded before cullers are applied.
     * 
     * If the cache is not valid, it will clear it and make a new cfdHash file for the CFD, so
     * any future cached features will be associated with the updated CFD.
     * @return False if the CFD has been modified (besides cullers) since the cache was made.
     *      True otherwise.
     */
    public boolean validateCFDCache() {
        long currentHash = cfd.longHash(EnumSet.of(FeatureSetElement.CANONICIZERS, FeatureSetElement.EVENT_DRIVERS, FeatureSetElement.NORMALIZATION));
        File cacheDir = new File(JSANConstants.JSAN_CACHE + "_" + cfd.getName());
        File cacheFile = new File(cacheDir, "cfdHash.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(cacheFile));
        } catch (FileNotFoundException e) {
            cacheDir.mkdirs();
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(cacheFile));
                writer.write(currentHash + "\n");
                writer.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            setCacheValid(false);
            return false;
        }
        long cachedHash = 0;
        try {
            cachedHash = Long.parseLong(reader.readLine());
            reader.close();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
            setCacheValid(false);
            return false;
        }
        if (cachedHash == currentHash) {
            setCacheValid(true);
            return true;
        } else {
            // Delete the cache and create a new one.
            // It is necessary to delete the entire cache (instead of just letting the cache
            // files be overwritten), because there may be some cache files not being used in this
            // case, and if some are not overwritten, but the CFD hash is updated, an invalid
            // cache could be used at some point thereafter.
            
            deleteRecursive(cacheDir);
            cacheDir.mkdirs();
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(cacheFile));
                writer.write(currentHash + "\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setCacheValid(false);
            return false;
        }
    }
    
    // Set whether or not the CFD cache is valid
    private void setCacheValid(boolean isValid) {
        isCacheValid = isValid;
    }
    
    public void setChunkDocs(boolean chunkDocs){
        if (chunkDocs)
            preferences.setPreference("chunkDocs", "1");
        else 
            preferences.setPreference("chunkDocs", "0");
    }

    public boolean isChunkingDocs() {
        return preferences.getBoolPreference("chunkDocs");
    }
}

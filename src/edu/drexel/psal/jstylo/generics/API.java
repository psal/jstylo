package edu.drexel.psal.jstylo.generics;

import java.util.ArrayList;
import java.util.List;

import weka.core.*;

import com.jgaap.generics.*;

/**
 * 
 * Interface for the JStylo feature extraction methods.<br>
 * Low-level methods that are used to extract and process features.<br>
 * Methods are also provided to convert the extracted features for particular documents into weka Attributes and unite them under an Instances object.<br>
 * @author Travis Dutko
 */
public interface API {

	/**
	 * Extracts the List of EventSets from a document using the provided CumulativeFeatureDriver.<br>
	 * @param document the document to have features extracted and made into event sets
	 * @param cumulativeFeatureDriver the driver containing the features to be extracted and the functionality to do so
	 * @param loadDocContents whether or not the document contents are already loaded into the object
	 * @return the List of EventSets for the document
	 */ 
	public List<EventSet> extractEventSets(Document document,
			CumulativeFeatureDriver cumulativeFeatureDriver, boolean loadDocContents) throws Exception;
	
	/**
	 * Determines which EventSets to use for the given documents based on the chosen cullers.<br>
	 * @param eventSets A List which contains Lists of EventSets (represents a list of documents' EventSets0
	 * @param cumulativeFeatureDriver the driver with the culling functionality
	 * @return The culled List of Lists of EventSets created from eventSets
	 * @throws Exception
	 */
	public List<List<EventSet>> cull(List<List<EventSet>> eventSets,
			CumulativeFeatureDriver cumulativeFeatureDriver) throws Exception;
	
	/**
	 * Goes over the culled List of Lists of EventSets and determines which events are histograms and which have a single
	 * numerical value.<br> Uses the information to prepare a List of EventSets to extract from the test document(s).
	 * @param culledEventSets The culled List of Lists of EventSets
	 * @param cumulativeFeatureDriver The driver used to extract the EventSets
	 * @return The List of EventSet to extract from the test document(s) 
	 * @throws Exception
	 */
	public List<EventSet> getRelevantEvents(List<List<EventSet>> culledEventSets,
			CumulativeFeatureDriver cumulativeFeatureDriver) throws Exception;
	
	/**
	 * Generates the List of Attributes from the List of Lists of EventSets that will be used to create the Instances object.<br>
	 * @param culledEventSets The culled list of EventSets that have been gathered from the document set
	 * @return A List of Attribute which will be used to create the Instances object 
	 * @throws Exception
	 */
	public ArrayList<Attribute> getAttributeList(
			List<List<EventSet>> culledEventSets, List<EventSet> relevantEvents, CumulativeFeatureDriver cfd, boolean hasDocTitles) throws Exception;
	
	/**
	 * Creates an Instance object from a document's extracted features and the relevant event list.<br>
	 * @param attributes the data used to construct the Instance object
	 * @param cumulativeFeatureDriver driver used to determine the type of feature being added
	 * @param documentData used to determine the values to assign each attribute
	 * @return The instance object representing this document
	 * @throws Exception
	 */
	public Instance createInstance(List<Attribute> attributes,
			List<EventSet> relevantEvents,
			CumulativeFeatureDriver cumulativeFeatureDriver,
			List<EventSet> documentData,
			boolean isSparse, boolean hasDocTitles) throws Exception;
	
	/**
	 * Normalizes all of the features of the specified instance.<br>
	 * Does not support global normalization baselines!<br>
	 * @param cumulativeFeatureDriver the driver used to tell what to normalize and in what fashion. 
	 * @param instance the object to be normalized
	 * @throws Exception
	 */
	public void normInstance(CumulativeFeatureDriver cumulativeFeatureDriver,
			Instance instance, List<EventSet> documentData, boolean hasDocTitles, List<Attribute> attributes) throws Exception;
	
	/**
	 * Calculates InfoGain on the instances to provide information on how useful each feature was to identifying the documents.<br>
	 * @param insts the instances to calculate over
	 * @return a two-dimensional sorted array with one element per feature. It is sorted via how useful each feature was and stores the index.
	 * @throws Exception
	 */
	public double[][] calcInfoGain(Instances insts) throws Exception;
	
	/**
	 * Removes all but the top N features (as returned by calcInfoGain) from the Instances object.<br>
	 * @param the indices and infoGain values of all attributes
	 * @param insts the instances to remove infoGain from
	 * @param n the number of features to keep
	 * @throws Exception
	 */
	public double[][] applyInfoGain(double[][] sortedFeatures, Instances insts, int n)
			throws Exception;
	
	/**
	 * Removes all but the top N features from a single Instance object.<br>
	 * @param sortedFeatures
	 * @param inst
	 * @param n
	 * @throws Exception
	 */
	public void applyInfoGain(double[][] sortedFeatures, Instance inst, int n) throws Exception;

	/**
	 * Culls the test set using the relevant Events extracted from the training data.<br>
	 * @param relevantEvents the features from the EventSets which are going to be evaluated
	 * @param eventSetsToCull The test documents to be culled
	 * @return the culled test documents
	 * @throws Exception
	 */
	public List<EventSet> cullWithRespectToTraining(
			List<EventSet> relevantEvents, List<EventSet> eventSetsToCull,
			CumulativeFeatureDriver cumulativeFeatureDriver)
			throws Exception;
	
}
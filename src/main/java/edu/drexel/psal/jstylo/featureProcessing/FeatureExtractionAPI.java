package edu.drexel.psal.jstylo.featureProcessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventHistogram;
import com.jgaap.generics.EventSet;

import edu.drexel.psal.JSANConstants;
import edu.drexel.psal.jstylo.eventDrivers.CharCounterEventDriver;
import edu.drexel.psal.jstylo.eventDrivers.LetterCounterEventDriver;
import edu.drexel.psal.jstylo.eventDrivers.SentenceCounterEventDriver;
import edu.drexel.psal.jstylo.eventDrivers.SingleNumericEventDriver;
import edu.drexel.psal.jstylo.eventDrivers.WordCounterEventDriver;
import edu.drexel.psal.jstylo.generics.Logger;

/**
 * 
 * This API provides methods for extracting document data from a single document at a time, and then processing that single document's data.
 * 
 * @author Travis Dutko
 */
public class FeatureExtractionAPI {

	/**
	 * Extracts the List of EventSets from a document using the provided CumulativeFeatureDriver.<br>
	 * @param document the document to have features extracted and made into event sets
	 * @param cumulativeFeatureDriver the driver containing the features to be extracted and the functionality to do so
	 * @param loadDocContents whether or not the document contents are already loaded into the object
	 * @return the List of EventSets for the document
	 */ 
	public List<EventSet> extractEventSets(Document document,
			CumulativeFeatureDriver cumulativeFeatureDriver, boolean loadDocContents, boolean isUsingCache) throws Exception {

	    List<EventSet> generatedEvents = new ArrayList<EventSet>();
	    
        if (isUsingCache) {
            File cacheDir = new File(JSANConstants.JSAN_CACHE + "_" + cumulativeFeatureDriver.getName() + "/");

            File authorDir = null;
            if (document.getAuthor().equals(JSANConstants.DUMMY_NAME)) {
                authorDir = new File(cacheDir, "you");
            } else {
                authorDir = new File(cacheDir, "_" + document.getAuthor());
            }

            File documentFile = new File(authorDir, document.getTitle() + ".cache");
            generatedEvents = getCachedFeatures(document, documentFile);
            if (generatedEvents == null) {
                // delete the cache for this document! It is invalid
                documentFile.delete();
                // program will continue as normal, extracting events
            } else {
                // return the cached features
                return generatedEvents;
            }
        }
		
		// Extract the Events from the documents
		try {
			generatedEvents = cumulativeFeatureDriver.createEventSets(document, loadDocContents,isUsingCache);
		} catch (Exception e) {
			Logger.logln("Failed to extract events from documents!");
			throw e;
		}
		// create metadata event to store document information
		EventSet documentInfo = new EventSet();
		documentInfo.setEventSetID("<DOCUMENT METADATA>");

		/*
		 * Metadata Event format:
		 * 
		 * EventSetID: "<DOCUMENT METADATA>" Event at Index: 0 : author 1 : title 2 : Sentences in document 3 : Words in document 4 : Characters in
		 * document 5 : Letters in document
		 */

		// Extract document title and author
		Event authorEvent = new Event(document.getAuthor());
		// Event titleEvent = new Event(document.getFilePath());
		Event titleEvent = new Event(document.getTitle());
		documentInfo.addEvent(authorEvent);
		documentInfo.addEvent(titleEvent);

		// Extract normalization baselines
		// Sentences in doc
		{
			Document doc = null;
			SingleNumericEventDriver counter = new SentenceCounterEventDriver();
			doc = document;
			Event tempEvent = null;

			if (!loadDocContents)
				try {
					doc.load();
					tempEvent = new Event("" + (int) counter.getValue(doc));
				} catch (Exception e) {
					Logger.logln("Failed to extract sentence count from document!");
					throw new Exception();
				}

			documentInfo.addEvent(tempEvent);
		}

		// Words in doc
		{
			Document doc = null;
			SingleNumericEventDriver counter = new WordCounterEventDriver();
			doc = document;
			Event tempEvent = null;
			try {
				if (!loadDocContents)
					doc.load();
				tempEvent = new Event("" + (int) counter.getValue(doc));
			} catch (Exception e) {
				Logger.logln("Failed to extract word count from document!");
				throw new Exception();
			}
			documentInfo.addEvent(tempEvent);
		}

		// Characters in doc
		{
			Document doc = null;
			SingleNumericEventDriver counter = new CharCounterEventDriver();
			doc = document;
			Event tempEvent = null;
			try {
				if (!loadDocContents)
					doc.load();
				tempEvent = new Event("" + (int) counter.getValue(doc));
			} catch (Exception e) {
				Logger.logln("Failed to extract character count from document!");
				throw new Exception();
			}
			documentInfo.addEvent(tempEvent);
		}

		// Letters in doc
		{
			Document doc = null;
			SingleNumericEventDriver counter = new LetterCounterEventDriver();
			doc = document;
			Event tempEvent = null;
			try {
				if (!loadDocContents)
					doc.load();
				tempEvent = new Event("" + (int) counter.getValue(doc));
			} catch (Exception e) {
				Logger.logln("Failed to extract character count from document!");
				throw new Exception();
			}
			documentInfo.addEvent(tempEvent);
		}

		// add the metadata EventSet to the List<EventSet>
		generatedEvents.add(documentInfo);

		// return the List<EventSet>
		return generatedEvents;
	}

	/**
	 * Determines which EventSets to use for the given documents based on the chosen cullers.<br>
	 * @param eventSets A List which contains Lists of EventSets (represents a list of documents' EventSets0
	 * @param cumulativeFeatureDriver the driver with the culling functionality
	 * @return The culled List of Lists of EventSets created from eventSets
	 * @throws Exception
	 */
	public List<List<EventSet>> cull(List<List<EventSet>> eventSets,
			CumulativeFeatureDriver cumulativeFeatureDriver) throws Exception {

		// a hacky workaround for the bug in the eventCuller. Fix that
		// later then remove these
		ArrayList<String> IDs = new ArrayList<String>();
		for (EventSet es : eventSets.get(0)) {
			IDs.add(es.getEventSetID());
		}
		
		//remove the metdata prior to culling
		ArrayList<EventSet> docMetaData = new ArrayList<EventSet>();
		for (List<EventSet> les : eventSets){
			docMetaData.add(les.remove(les.size()-1));
		}
		
		//cull the events
		List<List<EventSet>> culledEventSets = CumulativeEventCuller.cull(
				eventSets, cumulativeFeatureDriver);

		//add the metadata back in
		int index = 0;
		for (List<EventSet> les : culledEventSets){
			les.add(docMetaData.get(index));
			index++;
		}
		
		// a hacky workaround for the bug in the eventCuller. Fix that
		// later then remove these
		for (int j1 = 0; j1 < culledEventSets.size(); j1++) {
			for (int iterator = 0; iterator < culledEventSets.get(j1).size(); iterator++) {
				culledEventSets.get(j1).get(iterator)
						.setEventSetID(IDs.get(iterator));
			}
		}
		
		//return culled events
		return culledEventSets;
	}

	/**
	 * Goes over the culled List of Lists of EventSets and determines which events are histograms and which have a single
	 * numerical value.<br> Uses the information to prepare a List of EventSets to extract from the test document(s).
	 * @param culledEventSets The culled List of Lists of EventSets
	 * @param cumulativeFeatureDriver The driver used to extract the EventSets
	 * @return The List of EventSet to extract from the test document(s) 
	 * @throws Exception
	 */
	public List<EventSet> getRelevantEvents(
			List<List<EventSet>> culledEventSets,
			CumulativeFeatureDriver cumulativeFeatureDriver) throws Exception {

		//remove the metadata prior to generating the relevantEvents
		ArrayList<EventSet> docMetaData = new ArrayList<EventSet>();
		for (List<EventSet> les : culledEventSets){
			docMetaData.add(les.remove(les.size()-1));
		}
		
		//initialize the EventSet list
		List<EventSet> relevantEvents = new LinkedList<EventSet>();

		// iterate over the List of Lists
		for (List<EventSet> l : culledEventSets) {
			// iterate over each inner list's eventSets
			int featureIndex = 0;
			for (EventSet esToAdd : l) {
				// whether or not to add the event set to the list (if false, it
				// is already on the list)
				boolean add = true;;

				for (EventSet esl : relevantEvents) {
					// this should compare the category/name of the event set
					if (esToAdd.getEventSetID().equals(esl.getEventSetID())) {
						add = false;
						break;
					}
				}

				// this event set isn't on the list at all, just add it (which
				// also adds its internal events) to the list
				if (add) {
					EventSet temp = new EventSet();
					temp.setEventSetID(esToAdd.getEventSetID());

					//for all of the events
					for (Event e : esToAdd) {
						boolean absent = true;
						//check to see if it's been added yet or not
						for (Event ae : temp) {
							if (ae.getEvent().equals(e.getEvent())) {
								absent = false;
								break;
							}
						}
						//if it has not been added, add it
						if (absent) {
							if (!cumulativeFeatureDriver.featureDriverAt(
									featureIndex).isCalcHist())
								temp.addEvent(new Event("{-}"));
							else
								temp.addEvent(e);
						}
					}

					relevantEvents.add(temp);
				} else {
					// go through this eventSet and add any events to the
					// relevant EventSet if they aren't already there.
					if (cumulativeFeatureDriver.featureDriverAt(featureIndex)
							.isCalcHist()) {
						for (Event e : esToAdd) {
							boolean toAdd = true;
							//for all events in the relecant list
							for (Event re : relevantEvents.get(featureIndex)) {
								//if it's already there, don't add it
								if (e.getEvent().equals(re.getEvent())) {
									toAdd = false;
									break;
								}
							}
							//add it if it isn't there
							if (toAdd) {
								relevantEvents.get(featureIndex).addEvent(e);
							}
						}
					}
				}
				featureIndex++;
			}
		}
		
		//add the metadata back in
		int index = 0;
		for (List<EventSet> les : culledEventSets){
			les.add(docMetaData.get(index));
			index++;
		}
		
		return relevantEvents;
	}

	/**
	 * Extracts a list of all features to be used for analysis.
	 * @param culledEventSets
	 * @param relevantEvents
	 * @param cumulativeFeatureDriver
	 * @return
	 * @throws Exception
	 */
	public List<String> getFeatureList(List<List<EventSet>> culledEventSets,List<EventSet> relevantEvents,CumulativeFeatureDriver cumulativeFeatureDriver) throws Exception{
	    
        //remove the metdata prior to generating attribute list
        ArrayList<EventSet> docMetaData = new ArrayList<EventSet>();
        for (List<EventSet> les : culledEventSets){
            docMetaData.add(les.remove(les.size()-1));
        }
        
        //initialize useful things
        int numOfFeatureClasses = relevantEvents.size();
        List<EventSet> list;
        
        List<String> features = new ArrayList<String>(numOfFeatureClasses);

        // initialize list of sets of events, which will eventually become the
        // attributes
        List<EventSet> allEvents = new ArrayList<EventSet>(numOfFeatureClasses);
        
        //Neither the doc title nor the author is in the List<List<EventSet>>, so this should work fine
        for (int currEventSet = 0; currEventSet < numOfFeatureClasses; currEventSet++){
            // initialize relevant list of event sets and histograms
            list = new ArrayList<EventSet>();
            for (int i = 0; i < numOfFeatureClasses; i++)
                list.add(relevantEvents.get(i));
            
            //initialize eventSet
            EventSet events = new EventSet();
            events.setEventSetID(relevantEvents.get(currEventSet)
                    .getEventSetID());

            if (cumulativeFeatureDriver.featureDriverAt(currEventSet)
                    .isCalcHist()) { //histogram feature

                // generate event histograms and unique event list
                EventSet eventSet = list.get(currEventSet);
                for (Event event : eventSet) {
                    events.addEvent(event);
                }
                allEvents.add(events);

            } else { // one unique numeric event

                // generate sole event (give placeholder value)
                Event event = new Event("{-}");
                events.addEvent(event);
                allEvents.add(events);
            }
        }
        
        // Adds all of the events to the fast vector
        int featureIndex = 0;
        for (EventSet es : allEvents) {
            Iterator<Event> iterator = es.iterator();
            if (cumulativeFeatureDriver.featureDriverAt(featureIndex)
                    .isCalcHist()) {
                if (iterator.hasNext()){
                    //grab first event; there should be at least one
                    Event nextEvent = (Event) iterator.next();
                    //get and add all middle events if they exist
                    while (iterator.hasNext()) {
                        features.add(nextEvent.getEvent());
                        nextEvent = (Event) iterator.next();
                    }
                    //add the last event
                    features.add(nextEvent.getEvent());
                }
            } else {
                features.add(es.getEventSetID());
            }
            featureIndex++;
        }
        
        //add the metadata back in
        int index = 0;
        for (List<EventSet> les : culledEventSets){
            les.add(docMetaData.get(index));
            index++;
        }
        
        return features;
	}

	/**
	 * Converts the extracted document information into a JStylo DataMap
	 * @param features
	 * @param relevantEvents
	 * @param cumulativeFeatureDriver
	 * @param documentData
	 * @return
	 */
	public ConcurrentHashMap<Integer,Double> createDocMap(List<String> features,
	        List<EventSet> relevantEvents,
            CumulativeFeatureDriver cumulativeFeatureDriver,
            List<EventSet> documentData){
	    
        // generate training instances
        ConcurrentHashMap<Integer,Double> documentMap = new ConcurrentHashMap<Integer,Double>();
        
        //remove metadata event
        EventSet metadata = documentData.remove(documentData.size()-1);
        
        //go through all eventSets in the document
        for (EventSet es: documentData){
            
            //initialize relevant information
            ArrayList<Integer> indices = new ArrayList<Integer>();
            ArrayList<Event> events = new ArrayList<Event>();
            EventHistogram currHistogram = new EventHistogram();
            
            //whether or not we actually need this eventSet
            boolean eventSetIsRelevant = false;
            
            //find out if it is a histogram or not
            if (cumulativeFeatureDriver.featureDriverAt(
                    documentData.indexOf(es)).isCalcHist()) {
                
                //find the event set in the list of relevant events
                for (EventSet res : relevantEvents) {
                    if (es.getEventSetID().equals(res.getEventSetID())) {
                        eventSetIsRelevant = true;
                        break;
                    }
                }
                
                //if it is relevant
                if (eventSetIsRelevant) {

                    // find the indices of the events
                    // and count all of the events
                    for (Event e : es) {
                        int currIndex=0;
                        boolean hasInner = false;

                        //for the events n the set
                        for (EventSet res : relevantEvents) {
                            boolean found = false;
                            for (Event re : res) {
                                hasInner = true;
                                
                                //if they are the same event
                                if (e.getEvent().equals(re.getEvent())) {
                                    boolean inList = false;
                                    for (Event el : events) {
                                        if (el.getEvent().equals(e.getEvent())) {
                                            inList = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!inList) {
                                        indices.add(currIndex);
                                        events.add(e);
                                    }
                                    //Old location revert if change breaks
                                    currHistogram.add(e);
                                    found = true;
                                }
                                if (found){
                                    break;
                                }
                                currIndex++;
                            }
                            if (found){
                                break;
                            }
                            
                            //if there's no inner, it was a non-hist feature.
                            //increment by one
                            if (!hasInner){
                                currIndex++;
                            }
                        }
                    }
                    //calculate/add the histograms
                    int index = 0;
                    for (Integer i: indices){
                        documentMap.put(i, 0.0+currHistogram.getAbsoluteFrequency(events.get(index)));
                        index++;
                    }
                    
                }
            } else { //non histogram feature
                
                //initialize the index
                int nonHistIndex = 0;
                
                //find the indices of the events
                //and count all of the events
                for (EventSet res : relevantEvents) {
                    
                    if (es.getEventSetID().equals(res.getEventSetID())){
                        break;
                    }
                    
                    //count to find the index
                    boolean hasInner = false;
                    for (@SuppressWarnings("unused") Event re : res) {
                        hasInner = true;
                        nonHistIndex++;
                    }
                    
                    //if ther's no inner feature, incrememnt by one; we just passed a non-histogram
                    if (!hasInner)
                        nonHistIndex++;
                }

                //Extract and add the event             
                String eventString = es.eventAt(0).getEvent();
                int startIndex = eventString.indexOf("{");
                int endIndex = eventString.indexOf("}");
                eventString = eventString.substring(startIndex+1,endIndex);

                double value = Double.parseDouble(eventString);
                documentMap.put(nonHistIndex, value);
            }
        }
        //add metadata back. Not sure if necessary
        documentData.add(metadata);
        
	    return documentMap;
	}

	/**
	 * Normalizes all of the extracted feature data for a specific document.
	 * @param cfd
	 * @param docmap
	 * @param documentData
	 * @param features
	 */
	public void normDocData(CumulativeFeatureDriver cfd, ConcurrentHashMap<Integer,Double> docmap, List<EventSet> documentData, List<String> features){

        int i;
        int numOfFeatureClasses = cfd.numOfFeatureDrivers();

        int sentencesPerDoc = Integer.parseInt(documentData.get(documentData.size()-1).eventAt(2).getEvent());
        int wordsPerDoc = Integer.parseInt(documentData.get(documentData.size()-1).eventAt(3).getEvent());
        int charsPerDoc = Integer.parseInt(documentData.get(documentData.size()-1).eventAt(4).getEvent());
        int lettersPerDoc = Integer.parseInt(documentData.get(documentData.size()-1).eventAt(5).getEvent());
        int[] featureClassAttrsFirstIndex = new int[numOfFeatureClasses + 1];
        
        // initialize vector size (including authorName and title if required)
        // and first indices of feature classes array
        int vectorSize = 0;
        for (i = 0; i < numOfFeatureClasses; i++) {
            
            String featureDriverName = cfd.featureDriverAt(i).displayName()
                    .replace(" ", "-");
            
            String nextFeature = features.get(vectorSize).replace(" ", "-");
            
            featureClassAttrsFirstIndex[i] = vectorSize;
            while (nextFeature.contains(featureDriverName)) {
                vectorSize++;
                if (vectorSize == numOfFeatureClasses)
                    break;
                nextFeature = features.get(vectorSize);
            }
        }
        
        //add the end index
        featureClassAttrsFirstIndex[featureClassAttrsFirstIndex.length-1] = features.size();
        
        // normalizes features
        for (i = 0; i < numOfFeatureClasses; i++) {

            NormBaselineEnum norm = cfd.featureDriverAt(i).getNormBaseline();
            double factor = cfd.featureDriverAt(i).getNormFactor();
            int start = featureClassAttrsFirstIndex[i], end = featureClassAttrsFirstIndex[i + 1], k;
            if (norm == NormBaselineEnum.SENTENCES_IN_DOC) {
                // use sentencesInDoc
                if (!cfd.featureDriverAt(i).isCalcHist()) {
                    docmap.put(start, docmap.get(start) * factor / ((double) sentencesPerDoc));
                } else {
                    for (k = start; k < end; k++)
                        docmap.put(k, docmap.get(k) * factor / ((double) sentencesPerDoc));
                }
            } else if (norm == NormBaselineEnum.WORDS_IN_DOC) {
                // use wordsInDoc
                if (!cfd.featureDriverAt(i).isCalcHist()) {
                    docmap.put(start, docmap.get(start) * factor / ((double) wordsPerDoc));
                } else {
                    for (k = start; k < end; k++){
                        docmap.put(k, docmap.get(k) * factor / ((double) wordsPerDoc));
                    }
                }

            } else if (norm == NormBaselineEnum.CHARS_IN_DOC) {
                // use charsInDoc
                if (!cfd.featureDriverAt(i).isCalcHist()) {
                    docmap.put(start, docmap.get(start) * factor / ((double) charsPerDoc));
                } else {
                    for (k = start; k < end; k++)
                        docmap.put(k, docmap.get(k) * factor / ((double) charsPerDoc));
                }
            } else if (norm == NormBaselineEnum.LETTERS_IN_DOC) {
                // use charsInDoc
                if (!cfd.featureDriverAt(i).isCalcHist()) {
                    docmap.put(start, docmap.get(start) * factor / ((double) lettersPerDoc));
                } else {
                    for (k = start; k < end; k++)
                        docmap.put(k, docmap.get(k) * factor / ((double) lettersPerDoc));
                }
            }
        }
	}
	
	/**
	 * Culls the test set using the relevant Events extracted from the training data.<br>
	 * @param relevantEvents the features from the EventSets which are going to be evaluated
	 * @param eventSetsToCull The test documents to be culled
	 * @return the culled test documents
	 * @throws Exception
	 */
	public List<EventSet> cullWithRespectToTraining(
			List<EventSet> relevantEvents, List<EventSet> eventSetsToCull,
			CumulativeFeatureDriver cfd) throws Exception {
		List<EventSet> relevant = relevantEvents;
		int numOfFeatureClasses = eventSetsToCull.size()-1; //-1 to compensate for removing metadata
		int i;
		List<EventSet> culledUnknownEventSets = new LinkedList<EventSet>();

		//remove the metadata prior to culling
		EventSet metadata = eventSetsToCull.remove(eventSetsToCull.size()-1);
		
		// make sure all unknown sets would have only events that appear in the
		// known sets
		// UNLESS the event set contains a sole numeric value event - in that
		// case take it anyway
		for (i = 0; i < numOfFeatureClasses; i++) {
			if (cfd.featureDriverAt(i).isCalcHist()) {
				// initialize set of relevant events
				EventSet es = relevant.get(i);
				Set<String> relevantEventsString = new HashSet<String>(
						es.size());
				for (Event e : es)
					relevantEventsString.add(e.getEvent());

				// remove all non-relevant events from unknown event sets
				EventSet unknown;
				Event e;
				unknown = eventSetsToCull.get(i);
				Iterator<Event> iterator = unknown.iterator();
				Event next = null;

				// the test doc may not contain a given feature (ie it might not
				// have any semi-colons)
				if (iterator.hasNext())
					next = (Event) iterator.next();
				
				//while it has more of a feature
				while (iterator.hasNext()) {
					//copy the feature
					e = next;
					boolean remove = true;

					//check to see if the feature is relevant
					for (int l = 0; l < unknown.size(); l++) {
						try {
							if (e.equals(relevantEvents.get(i).eventAt(l))) {
								remove = false; //if it is, break 
								break;
							}
						} catch (IndexOutOfBoundsException iobe) {
							remove = true; //it is not relevant if we reach this point.
							break;
						}
					}

					//remove the feature if it isn't relevant
					if (remove) {
						iterator.remove();
					}
					
					//grab the next feature
					next = iterator.next();
				}
				
				//add the culled event set
				culledUnknownEventSets.add(unknown);

			} else { // one unique numeric event
				// add non-histogram if it is in the relevantEventSets list
				boolean isRelevant = false;
				
				for (EventSet res: relevantEvents){
					if (res.getEventSetID().equals(
							eventSetsToCull.get(i).getEventSetID())) {
						isRelevant = true;
						break;
					}
				}
				
				if (isRelevant)
					culledUnknownEventSets.add(eventSetsToCull.get(i));
			}
		}
		eventSetsToCull.add(metadata);
		culledUnknownEventSets.add(metadata);
		
		return culledUnknownEventSets;
	}

	/**
     * Loads the cached features for a given document
     * @param document
     * @param documentFile  The cache file for the document.
     * @return the cached features if possible. Null if a cache doesn't exist or it fails to get them.
     * @throws Exception 
     */
    private List<EventSet> getCachedFeatures(Document document, File documentFile) {
        List<EventSet> generatedEvents = null;
        BufferedReader reader = null;
        
        if (documentFile.exists() && !documentFile.isDirectory() && documentFile.canRead()) {
            try {
                reader = new BufferedReader(new FileReader(documentFile));
            } catch (FileNotFoundException e) {
                // shouldn't ever get here.. just put this here so I can keep track
                // of exceptions below.
                e.printStackTrace();
            }
        } else {
            return null;
        }
        
        try {
            // cachedPath is the path to the document that was used when the cache for that
            // document was created. cachedLastModified is the last modified time stamp on the
            // document that was cached.
            String cachedPath = reader.readLine();
            long cachedLastModified = Long.parseLong(reader.readLine());

            String path = document.getFilePath();
            File currDoc = new File(path);
            long lastModified = currDoc.lastModified();

            if (!(currDoc.getCanonicalPath().equals(cachedPath) && lastModified == cachedLastModified)) {
                // cache is invalid
                reader.close();
                return null;
            }
            String line = null;
            generatedEvents = new ArrayList<EventSet>();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                EventSet es = new EventSet();
                es.setAuthor(document.getAuthor());
                es.setDocumentName(document.getTitle());
                es.setEventSetID(line);
                
                String event = null;
                while ((event = reader.readLine()) != null) {
                    if (line.isEmpty())
                        continue;
                    if (event.equals(",")) //delimiter for event sets
                        break;
                    es.addEvent(new Event(event));
                }
                
                generatedEvents.add(es);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        return generatedEvents;
    }
    
    /**
     * Recursively delete contents of a directory (if f is a directory),
     * then delete f.
     * @param dir   the file/directory to delete
     * @return true if f was successfully deleted, false otherwise
     */
    public static boolean deleteRecursive(File f) {
        File cacheDir = new File(JSANConstants.JSAN_CACHE);
        try {
            if (f.getCanonicalPath().startsWith(cacheDir.getCanonicalPath())) {
                return deleteRecursiveUnsafe(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }    

    /**
     * Recursively delete contents of a directory (if f is a directory),
     * then delete f.
     * @param dir   the file/directory to delete
     * @return true if f was successfully deleted, false otherwise
     */
    private static boolean deleteRecursiveUnsafe(File f) {
        if (f.exists()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteRecursiveUnsafe(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return f.delete();
    }
    
}

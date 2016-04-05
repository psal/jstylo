package edu.drexel.psal.jstylo.featureProcessing;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import com.jgaap.generics.*;

import edu.drexel.psal.JSANConstants;
import edu.drexel.psal.jstylo.eventDrivers.StanfordDriver;
import edu.drexel.psal.jstylo.generics.Logger;

/**
 * The cumulative feature driver class is designed to create a concatenated result of several feature drivers.
 * For details about a feature driver, look into the documentation of {@see FeatureDriver}.
 * 
 * @author Ariel Stolerman
 *
 */
public class CumulativeFeatureDriver implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * Used when serializing it to XML. The initial size is
	 * initialized to this so it doesn't have to continuously find more memory.
	 */
	private static final int INITIAL_WRITE_SIZE = 4096;

	/* ======
	 * fields
	 * ======
	 */
	
	/**
	 * List of feature drivers.
	 */
	private List<FeatureDriver> features;
	
	/**
	 * Name of the feature set.
	 */
	private String name;
	
	/**
	 * Description of the feature set.
	 */
	private String description;
	
	/* ============
	 * constructors
	 * ============
	 */

	/**
	 * Creates a new cumulative feature driver.
	 */
	public CumulativeFeatureDriver() {
		features = new ArrayList<FeatureDriver>();
	}
	
	/**
	 * Copy constructor.
	 * @param other to copy.
	 */
	public CumulativeFeatureDriver(CumulativeFeatureDriver other) throws Exception
	{
		String path = "tmp.xml";
		File xml = new File(path);
		PrintWriter pw = new PrintWriter(xml);
		pw.print(other.toXMLString());
		pw.flush();
		pw.close();
		XMLParser parser = new XMLParser(path);
		CumulativeFeatureDriver generated = parser.cfd;
		this.name = generated.name;
		this.description = generated.description;
		this.features = generated.features;
		xml.delete();
	}

	/**
	 * Creates a new cumulative feature driver, with the given list of feature drivers.
	 * @param featureDrivers
	 * 		The list of feature drivers to be set.
	 */
	public CumulativeFeatureDriver(List<FeatureDriver> featureDrivers) {
		this.features = featureDrivers;
	}
	
	/**
	 * Constructor for CumulativeFeatureDriver from a given XML file.
	 * @param filename
	 * 		The name of the XML file to generate the feature set from.
	 * @throws Exception
	 */
	public CumulativeFeatureDriver(String filename) throws Exception {
		Logger.logln("Reading CumulativeFeatureDriver from "+filename);
		XMLParser parser = new XMLParser(filename);
		CumulativeFeatureDriver generated = parser.cfd;
		this.name = generated.name;
		this.description = generated.description;
		this.features = generated.features;
	}
	
	/* ==========
	 * operations
	 * ==========
	 */
	
	/**
	 * Returns a list of all event sets extracted per each feature driver.
	 * Does not make use of cache.
	 * @param doc
	 * 		Input document.
	 * @param usingCache
	 * 		Whether or not to cache the results to file.
	 * @return
	 * 		List of all event sets extracted per each event driver.
	 * @throws Exception 
	 */
	public List<EventSet> createEventSets(Document doc, boolean loadDocContents, boolean usingCache) throws Exception {
		
		List<EventSet> esl = new ArrayList<EventSet>();
		
		File docCache = null;
		File docOriginal = null;
		BufferedWriter writer = null;
		
		if (usingCache) {
			File cacheDir = new File(JSANConstants.JSAN_CACHE + "_" + getName() + "/");
			File authorDir = null;
			if (doc.getAuthor().equals(JSANConstants.DUMMY_NAME)) {
				authorDir = new File(cacheDir, "you");
			} else {
				authorDir = new File(cacheDir, "_" + doc.getAuthor());
			}

			if (!authorDir.exists()) {
				authorDir.mkdirs();
			}

			// We will start making the cache file in this method. The metadata
			// will be appended
			// in the Engine's extractEventSets() method..

			docCache = new File(authorDir, doc.getTitle() + ".cache");
			docOriginal = new File(doc.getFilePath());

			writer = new BufferedWriter(new FileWriter(docCache));
			writer.write(docOriginal.getCanonicalPath() + '\n');
			writer.write(Long.toString(docOriginal.lastModified()) + '\n');
		}
		
		for (int i=0; i<features.size(); i++) {
			EventDriver ed = features.get(i).getUnderlyingEventDriver();
			Document currDoc = doc instanceof StringDocument ?
					new StringDocument((StringDocument) doc) :
					new Document(doc.getFilePath(),doc.getAuthor(),doc.getTitle());
			
			if (loadDocContents)
				currDoc.readStringText(String.copyValueOf(doc.getProcessedText()));
					
			// apply canonicizers
			try {
				for (Canonicizer c: features.get(i).getCanonicizers())
					currDoc.addCanonicizer(c);
			} catch (NullPointerException e) {
				// no canonicizers
				Logger.logln("No canonicizers in use");
			}
			
			if (!loadDocContents) {
				try {
					currDoc.load();
				} catch (Exception e) {
					Logger.logln("Failed to load document contents!");
					if (usingCache)
						writer.close();
					throw new Exception();
				}
			}
			
			try {
				currDoc.processCanonicizers();
			} catch (LanguageParsingException | CanonicizationException e1) {
				Logger.logln("Failed to canonicize the document!");
				if (usingCache)
					writer.close();
				throw new Exception();
			}
			
			// extract event set
			String prefix = features.get(i).displayName().replace(" ", "-");
			EventSet tmpEs = null;
			try {
				tmpEs = ed.createEventSet(currDoc);
			} catch (EventGenerationException e1) {
				Logger.logln("Failed to create EventSet!");
				if (usingCache)
					writer.close();
				throw new Exception();
			}
			tmpEs.setEventSetID(features.get(i).getName());
			EventSet es = new EventSet();
			es.setAuthor(doc.getAuthor());
			es.setDocumentName(doc.getTitle());
			es.setEventSetID(tmpEs.getEventSetID());
			if (usingCache) {
				writer.write(es.getEventSetID() + "\n");
			}
			
			// TODO: I tried to use a hash map to compress the output cache file. 
			//		This hash map did not work. It caused inconsistencies between cached features
			//		and extracted features. Not sure why, but I wrote tests for it in CacheTests.
			//		Any further optimizations / compressions must pass those tests.
			
			for (Event e: tmpEs){
				String event = e.getEvent();

				es.addEvent(new Event(prefix+"{"+event+"}"));
				if (usingCache) {
					writer.write(prefix+"{"+event+"}\n");
				}
			}
			
			if (usingCache) {
					writer.write(",\n");
			}

			esl.add(es);
		}
		if (usingCache)
			writer.close();
		return esl;
	}
	
	public void clean(){
		int n = numOfFeatureDrivers();
		for (int i = 0; i < n; i++){
			FeatureDriver fd = removeFeatureDriverAt(0);
			{
				List<EventCuller> cullers = fd.getCullers();
				cullers.clear();
				cullers = null;
				
				EventDriver eventDriver = fd.getUnderlyingEventDriver();
				if (eventDriver instanceof StanfordDriver){
					((StanfordDriver) eventDriver).destroyTagger();
				}
				
				eventDriver = null;
			}
			fd = null;
		}
	}
	
	/* =======
	 * queries
	 * =======
	 */
	
	/**
	 * Returns the feature driver at the given index from the list of feature drivers.
	 * @param i
	 * 		The index of the desired feature driver in the list of feature drivers.
	 * @return
	 * 		The feature driver at the given index in the list of feature drivers.
	 */
	public FeatureDriver featureDriverAt(int i) {
		return features.get(i);
	}
	
	/**
	 * Returns the display name of the cumulative feature driver.
	 * @return
	 * 		The display name of the cumulative feature driver.
	 */
	public String displayName() {
		String res = "Cumulative feature driver: ";
		for (FeatureDriver fd: features) {
			res += fd.getUnderlyingEventDriver().displayName()+", ";
		}
		return res.substring(0, res.length()-2);
	}
	
	/**
	 * Returns the size of the feature drivers list.
	 * @return
	 * 		The size of the feature drivers list.
	 */
	public int numOfFeatureDrivers() {
		return features.size();
	}
	
	
	/* =======
	 * setters
	 * =======
	 */
	
	/**
	 * Sets the name of the feature set.
	 * @param name
	 * 		The name to be set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Sets the description of the feature set.
	 * @param description
	 * 		The description to be set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Adds the given feature driver to the list of feature drivers and returns its position in the list.
	 * If the add operation failed, returns -1.
	 * @param ed
	 * 		The feature driver to be added to the list of feature drivers.
	 * @return
	 * 		The position of the given feature driver in the list of feature drivers. If the add operation
	 * 		failed, returns -1.
	 */
	public int addFeatureDriver(FeatureDriver fd) {
		if (features.add(fd))
			return features.indexOf(fd);
		else return -1;
		
	}
	
	/**
	 * Replaces the feature driver at the given index with the given feature driver and returns the old one.
	 * @param i
	 * 		The index to replace old feature driver at.
	 * @param fd
	 * 		The new feature driver to be placed at the given index. 
	 * @return
	 * 		The old feature driver at the given index.
	 */
	public FeatureDriver replaceFeatureDriverAt(int i, FeatureDriver fd) {
		FeatureDriver old = featureDriverAt(i);
		features.set(i, fd);
		return old;
	}
	
	/**
	 * Removes the feature driver in the given index.
	 * @param i
	 * 		The index to remove the feature driver from.
	 * @return
	 * 		The removed feature driver, or null if the index is out of bound.
	 */
	public FeatureDriver removeFeatureDriverAt(int i) {
		if (i >= 0 && i < features.size()) {
			return features.remove(i);
		} else {
			return null;
		}
	}
	
	
	/* =======
	 * getters
	 * =======
	 */
	
	/**
	 * Returns the name of the feature set.
	 * @return
	 * 		The name of the feature set.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the description of the feature set.
	 * @return
	 * 		The description of the feature set.
	 */
	public String getDescription() {
		return description;
	}
	
	
	/* ===========
	 * XML parsing
	 * ===========
	 */
	
	/**
	 * Writes the cumulative feature driver in XML format to the given path.
	 * @param path
	 * 		The path of the output XML file.
	 * @return
	 * 		True iff the write succeeded.
	 */
	public boolean writeToXML(String path) {
		if (!path.endsWith(".xml"))
			path = path+".xml";
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(path));
			bw.write(toXMLString());
			bw.close();
		} catch (IOException e) {
			if (bw != null)
				try {
					bw.close();
				} catch (IOException ex) {}
			return false;
		}
		return true;
	}
	
    /**
     * Choose what is serialized from the feature set.
     * @author Andrew DiNunzio
     *
     */
    public enum FeatureSetElement {
        EVENT_DRIVERS,
        CANONICIZERS,
        CULLERS,
        NORMALIZATION;
        
        public static final EnumSet<FeatureSetElement> ALL = EnumSet.allOf(FeatureSetElement.class);
    }
	
	/**
	 * Saves the cumulative feature driver in XML format to the given path.
	 * @param path
	 * 		The path to save to.
	 */
	public String toXMLString() {
		return toXMLString(FeatureSetElement.ALL);
	}
	
    /**
     * Serialize part of the CFD based on the featureFlags.
     * 
     * Always specify ALL when writing to file.
     */
    private String toXMLString(EnumSet<FeatureSetElement> featureFlags) {
        StringBuilder res = new StringBuilder(INITIAL_WRITE_SIZE);

        res.append("<?xml version=\"1.0\"?>\n");
        res.append("<feature-set name=\"");
        	res.append(name);
        	res.append("\">\n");
        res.append("\t<description value=\"");
        	res.append(description);
        	res.append("\"/>\n");

        FeatureDriver fd;
        for (int i=0; i<features.size(); i++) {
            fd = features.get(i);
            res.append("\t<feature name=\"");
            	res.append(fd.getName());
            	res.append("\" calc_hist=\"");
            	res.append(fd.isCalcHist());
            	res.append("\">\n");

            // description
            res.append("\t\t<description value=\"");
            	res.append(fd.getDescription());
            	res.append("\"/>\n");

            if (featureFlags.contains(FeatureSetElement.EVENT_DRIVERS)) {
                // event driver
                EventDriver ed = fd.getUnderlyingEventDriver();
                res.append("\t\t<event-driver class=\"");
                	res.append(ed.getClass().getName());
                	res.append("\">\n");
                // parameters
                for (Pair<String, FeatureDriver.ParamTag> param : FeatureDriver.getClassParams(ed.getClass().getName())) {
                	res.append("\t\t\t<param name=\"");
                		res.append(param.getFirst());
                		res.append("\" value=\"");
                		res.append(ed.getParameter(param.getFirst()));
                		res.append("\"/>\n");
                }
                res.append("\t\t</event-driver>\n");
            }
            
            if (featureFlags.contains(FeatureSetElement.CANONICIZERS)) {
                // canonicizers
                res.append("\t\t<canonicizers>\n");
                if (fd.getCanonicizers() != null) {
                    for (Canonicizer c : fd.getCanonicizers()) {
                        res.append("\t\t\t<canonicizer class=\"");
                        	res.append(c.getClass().getName());
                        	res.append("\">\n");
                        for (Pair<String, FeatureDriver.ParamTag> param : FeatureDriver.getClassParams(c.getClass().getName())) {
                            res.append("\t\t\t\t<param name=\"");
                            	res.append(param.getFirst());
                            	res.append("\" value=\"");
                            	res.append(c.getParameter(param.getFirst()));
                            	res.append("\"/>\n");
                        }
                        res.append("\t\t\t</canonicizer>\n");
                    }
                }
                res.append("\t\t</canonicizers>\n");
            }

            if (featureFlags.contains(FeatureSetElement.CULLERS)) {
                // cullers
            	res.append("\t\t<cullers>\n");
                if (fd.getCullers() != null) {
                    for (EventCuller ec: fd.getCullers()) {
                        res.append("\t\t\t<culler class=\"");
                        res.append(ec.getClass().getName());
                        res.append("\">\n");
                        for (Pair<String,FeatureDriver.ParamTag> param: FeatureDriver.getClassParams(ec.getClass().getName())) {
                            res.append("\t\t\t\t<param name=\"");
                            	res.append(param.getFirst());
                            	res.append("\" value=\"");
                            	res.append(ec.getParameter(param.getFirst()));
                            	res.append("\"/>\n");
                        }
                        res.append("\t\t\t</culler>\n");
                    }
                }
                res.append("\t\t</cullers>\n");
            }
            
            if (featureFlags.contains(FeatureSetElement.NORMALIZATION)) {
                // normalization
            	res.append("\t\t<norm value=\"");
            		res.append(fd.getNormBaseline());
            		res.append("\"/>\n");
                res.append("\t\t<factor value=\"");
                	res.append(fd.getNormFactor());
                	res.append("\"/>\n");
            }

            res.append("\t</feature>\n");
        }
        res.append("</feature-set>\n");
        
        return res.toString();
    }
	
	/**
	 * 
	 * @param featureFlags
	 * @return
	 */
    public long longHash(EnumSet<FeatureSetElement> featureFlags) {
        String xml = toXMLString(featureFlags);
        long hash = 0;
        long value;
        int length = xml.length();
        int i,j;
        for (i=0; i < length; i++) {
            value = xml.charAt(i);
            for (j = length-(i+1); j > 0; j--)
                value = (value << 5) - value;
            hash += value;
        }
        return hash;
    }
	
	
	/**
	 * XML parser to create a cumulative feature driver out of a XML file.
	 */
	private class XMLParser extends DefaultHandler {
		
		/* ======
		 * fields
		 * ======
		 */
		private CumulativeFeatureDriver cfd;
		private String filename;
		
		/* ============
		 * constructors
		 * ============
		 */
		public XMLParser(String filename) throws Exception {
			cfd = new CumulativeFeatureDriver();
			this.filename = filename;
			parse();
		}
		
		
		/* ==========
		 * operations
		 * ==========
		 */
		/**
		 * Parses the XML input file into a problem set.
		 * @throws Exception
		 * 		SAXException, ParserConfigurationException, IOException
		 */
		public void parse() throws Exception {
			
			//intialize the parser, parse the document, and build the tree
			DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
			DocumentBuilder dom = builder.newDocumentBuilder();
			org.w3c.dom.Document xmlDoc = dom.parse(filename);	
			xmlDoc.getDocumentElement().normalize();
			
			//create the feature set and intialize the cfd's name
			NodeList featureSet = xmlDoc.getElementsByTagName("feature-set");
			Element fs = (Element) xmlDoc.importNode(featureSet.item(0),false);
			cfd.setName(fs.getAttribute("name"));
			
			//load single value lists (ie lists where, any given feature set will have exactly one value
			NodeList descriptions = xmlDoc.getElementsByTagName("description");
			NodeList eventDrivers = xmlDoc.getElementsByTagName("event-driver");
			NodeList normValues = xmlDoc.getElementsByTagName("norm");
			NodeList normFactors = xmlDoc.getElementsByTagName("factor");
			
			//load the feature set description
			Element fsd = (Element) xmlDoc.importNode(descriptions.item(0),false);
			cfd.setDescription(fsd.getAttribute("value"));
			
			//get the list of features
			NodeList items = xmlDoc.getElementsByTagName("feature");
			//go over the nodes and get the information we want
			for (int i=0; i<items.getLength();i++){
				boolean successful = true;
				FeatureDriver fd = new FeatureDriver();
				try{
					//initialize this feature node, this feature element, and the feature driver
					Node currentNode = items.item(i);
					Element currentElement = (Element) xmlDoc.importNode(items.item(i), true);
					
					
					//add the information from this node to the feature driver
					fd.setName(currentElement.getAttribute("name"));
					if (currentElement.getAttribute("calc_hist").equals("false"))
						fd.setCalcHist(false);
					else
						fd.setCalcHist(true);
					
					//get all of the components
					
					//description
					Element currDesc = (Element) xmlDoc.importNode(descriptions.item(i+1), false);
					fd.setDescription(currDesc.getAttribute("value"));
				
					//event driver
					Node currEvNode = eventDrivers.item(i);
					Element currEvDriver = (Element) xmlDoc.importNode(eventDrivers.item(i), false);
					EventDriver ed = (EventDriver) Class.forName(currEvDriver.getAttribute("class")).newInstance();
					//check for args, adding them if necessary
					if (currEvNode.hasChildNodes()){
						NodeList params = currEvNode.getChildNodes();
						for (int k=0; k<params.getLength();k++){
							
							Node currPNode = params.item(k);
							if(!currPNode.getNodeName().equals("#text")){
								Element currParam = (Element) xmlDoc.importNode(params.item(k), false);
								if (currParam.hasAttribute("name")&&currParam.hasAttribute("value"))
									ed.setParameter(currParam.getAttribute("name"), currParam.getAttribute("value"));
							}
						}
					}
					fd.setUnderlyingEventDriver(ed);
					
					NodeList children = currentNode.getChildNodes(); //used for both canonicizers and cullers
					//canonicizer(s)
					//loop through the children until you find the canonicizers
					for (int j=0; j<children.getLength(); j++){
						Node current = children.item(j);
						if (!current.getNodeName().equals("#text")){
							if (current.getNodeName().equals("canonicizers")){
								if (current.hasChildNodes()){
									NodeList canonicizers = current.getChildNodes();
									//iterate over the canonicizers
									for (int k=0; k<canonicizers.getLength();k++){
										if (!canonicizers.item(k).getNodeName().equals("#text")){
											Element currCanon = (Element) xmlDoc.importNode(canonicizers.item(k), false);
											if (currCanon.hasAttribute("class"))
												fd.addCanonicizer((Canonicizer) Class.forName(currCanon.getAttribute("class")).newInstance());	
										}
									}
								}
								break; //once we're done with canonicizers we don't need to check anything else
							}
						}
					}
				
					//event culler(s)
					//look through the children of features until you find the cullers
					for (int j=0; j<children.getLength();j++){
						Node current = children.item(j);
						//then iterate over the individual cullers
						if (current.getNodeName().equals("cullers")){
							if (current.hasChildNodes()){ //if it has children, otherwise its just an empty tag
							
								//list of cullers to be passed to the fd
								LinkedList<EventCuller> cullers = new LinkedList<EventCuller>();
								NodeList evculls = current.getChildNodes();
							
								//go over the culler tags
								for (int k=0; k<evculls.getLength();k++){
									Node currEvCNode = evculls.item(k);
									if(!currEvCNode.getNodeName().equals("#text")){
										Element currEvCuller = (Element) xmlDoc.importNode(evculls.item(k), false);
										//if it is an actually culler and has contents, add it
										if (currEvCuller.hasAttribute("class")){
											//create the class
											EventCuller culler = (EventCuller) Class.forName(currEvCuller.getAttribute("class")).newInstance();
											//if it has children, they're arguments
											if (currEvCNode.hasChildNodes()){
												//take the list of args and iterate over it
												NodeList params = currEvCNode.getChildNodes();
												for (int m=0; m<params.getLength();m++){
													Node currPNode = params.item(m);
													//and if the args are useful
													if (!currPNode.getNodeName().equals("#text")){
														//add the args to the culler
														Element currParam = (Element) xmlDoc.importNode(params.item(m), true);
														if (currParam.hasAttribute("name") &&  currParam.hasAttribute("value")){
															culler.setParameter(currParam.getAttribute("name"), currParam.getAttribute("value"));
														}		
													}
												}
											}
											cullers.add(culler); //add the culler to the list
										}
									}
								}
								fd.setCullers(cullers);
							}
							break;
						}
					}
				
					//normalization value
					Element currNormV = (Element) xmlDoc.importNode(normValues.item(i), false);
					fd.setNormBaseline(NormBaselineEnum.valueOf(currNormV.getAttribute("value")));
					
					//normalization factor
					Element currNormF = (Element) xmlDoc.importNode(normFactors.item(i), false);
					fd.setNormFactor(Double.parseDouble(currNormF.getAttribute("value")));
					
					
				} catch (Exception e){
					successful = false;
					Element currentElement = (Element) xmlDoc.importNode(items.item(i), true);
					Logger.logln("Failed to load feature driver: "+currentElement.getAttribute("name"),Logger.LogOut.STDERR);
				}
				
				if (successful){
					//add the feature driver
					cfd.addFeatureDriver(fd);
				}
			}
		}
		
		/**
		 * Returns the generated cumulative feature driver.
		 * @return
		 * 		The generated cumulative feature driver.
		 */
		@SuppressWarnings("unused")
		public CumulativeFeatureDriver getCumulativeFeatureDriver() {
			return cfd;
		}
		
	}
}

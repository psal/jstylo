package edu.drexel.psal.jstylo.generics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.drexel.psal.jstylo.featureProcessing.FeatureData;

/**
 * This class is a generic class designed to hold all of the information extracted from documents prior to classification.
 * It consists of a few key data structures:
 * 
 * 1) A Massive nested map that keys authors to a map of documents (which is keyed on document title and mapped to a map of data)
 *      This is the data extracted from the documents
 *      
 * 2) A Map<Integer,String> which is the map of index to feature name. These indices correspond to the indices used as a key
 *      in the innermost map of the primary data structure.
 *      
 * This class is backed by ConcurrentHashMaps as it is intended to be multi-thread capable.
 * 
 * @author Travis Dutko
 */
public class DataMap {

    /**
     * A three-layered map containing all of the documents' feature information.
     * The Outer Map is a map of author names to document maps
     * The Middle Map is a map of document titles to feature maps
     * The Inner Map is a map of feature indices to the feature values
     */
    private ConcurrentHashMap<String, //outer layer is indexed on author identities
        ConcurrentHashMap<String, //middle layer is indexed on document titles
            DocumentData>> //data value is the double value for a given doc for a given author for that index feature
                    datamap;
    
    /**
     * The map of index to feature name.
     * Keying on index might seem weird, and maybe it is, but it makes a lot of operations easier
     */
    private Map<Integer,String> features;
    
    /**
     * Name of the data set
     */
    private String datasetName;
    
    public DataMap(String name, List<String> featureNames){
        datasetName = name;
        setFeatures(featureNames);
        datamap = new ConcurrentHashMap<String,ConcurrentHashMap<String,DocumentData>>();
    }
    
    /**
     * Add a potential author to the datamap
     * @param author
     */
    public void initAuthor(String author){
        ConcurrentHashMap<String,DocumentData> newAuthorMap = new ConcurrentHashMap<String,DocumentData>();
        datamap.put(author, newAuthorMap);
    }

    /**
     * Adds the data from a single document to the datamap.
     * @param author author to add to
     * @param documentTitle title of the document
     * @param map map of indices to values
     */
    public void addDocumentData(String author, String documentTitle, DocumentData docdata) {
        datamap.get(author).put(documentTitle, docdata);
    }
    
    /**
     * 
     * @return the internal datamap
     */
    public ConcurrentHashMap<String,ConcurrentHashMap<String,DocumentData>> getDataMap(){
        return datamap;
    }

    /**
     * 
     * @return the feature map
     */
    public Map<Integer,String> getFeatures() {
        return features;
    }

    /**
     * Sets the feature map - replacing it if it is already present
     * @param featureList
     */
    private void setFeatures(List<String> featureList) {
        if (features != null){
            features.clear();
        }
        
        this.features = new HashMap<Integer,String>();
        int i = 0;
        for (String feature : featureList){
            features.put(i, feature);
            i++;
        }
    }
    
    /**
     * Removes a set of feature indexes and updates the feature set and document data as appropriate
     * NOTE: this edits in place, so the old datamap is overwritten!
     * @param indicesToRemove
     */
    public void removeFeatures(Integer[] indicesToRemove){
        //empty feature list
        List<String> newFeatures = new ArrayList<String>();
        List<Integer> match = new ArrayList<Integer>();
        for (Integer inte : indicesToRemove)
            match.add(inte);
        for (Integer i = 0; i < features.size(); i++){
            if (!match.contains(i)) {
                newFeatures.add(features.get(i));
            } 
        }
        setFeatures(newFeatures);

        for (int i = 0; i < indicesToRemove.length; i++){

            Integer indexToRemove = indicesToRemove[i];
            
            for (String author : datamap.keySet()){ //for all authors
                for (String document: datamap.get(author).keySet()){ //for all of their documents
                    ConcurrentHashMap<Integer,FeatureData> docData = datamap.get(author).get(document).getDataValues();
                    //remove the index/feature if it is present
                    if (docData.containsKey(indexToRemove)){
                        docData.remove(indexToRemove);
                    }
                    //regardless, bump down the index of ALL other features
                    ConcurrentHashMap<Integer,FeatureData> newDocData = new ConcurrentHashMap<Integer,FeatureData>();
                    for (Integer key : docData.keySet()){
                        if (key < indexToRemove){ //if the index is under i, add as normal
                            newDocData.put(key, docData.get(key));
                        } else if (key > indexToRemove){ // if the index is over i, add it with a - 1
                            newDocData.put(key-1, docData.get(key));
                        }
                    }
                    datamap.get(author).get(document).replaceFeatureValues(newDocData);
                }
            }
                
            //adjust all future indices to remove to account for 
            for (int j = i+1; j < indicesToRemove.length; j++){
                if (indicesToRemove[i] <= indicesToRemove[j])
                    indicesToRemove[j] = indicesToRemove[j]-1;
            }
        }
    }

    /**
     * 
     * @return the number of documents present in this datamap
     */
    public int numDocuments(){
        int count = 0;
        for (String author : datamap.keySet()){
            count+=datamap.get(author).size();
        }
        return count;
    }
    
    /**
     * Writes the datamap to a dense CSV file
     * @param path location to write the file
     */
    public void saveDataMapToCSV(String path){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
            
            bw.write(datasetName+"\n");
            
            //first line is all of the feature labels
            String nextline = "authorName, document, ";
            for (Integer index : features.keySet()){
                nextline += features.get(index)+",";
            }
            bw.write(nextline+"\n");
            
            //each row is a document, organized by author
            for (String author: datamap.keySet()){
                for (String document : datamap.get(author).keySet()){
                    nextline =author+","+document+",";
                    ConcurrentHashMap<Integer,FeatureData> docdata = datamap.get(author).get(document).getDataValues();
                    for (int i = 0; i <features.size(); i++){
                        if (docdata.containsKey(i))
                            nextline+=docdata.get(i).getValue()+",";
                        else
                            nextline+="0,";
                    }
                    nextline = nextline.substring(0,nextline.length()-1);
                    nextline+="\n";
                    bw.write(nextline);
                }
            }
            
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write out to CSV file.");
        }
    }
    
    public Integer getFeatureIndex(String featureName){
        if (!features.containsValue(featureName))
            return -1;
        for (Integer index : features.keySet()){
            if (features.get(index).equals(featureName))
                return index;
        }
        return -1;
    }
    
    public List<String> getDocumentTitles(){
        List<String> titles = new ArrayList<String>();
        for (String author : datamap.keySet()){
            for (String doctitle : datamap.get(author).keySet()){
                titles.add(doctitle);
            }
        }
        return titles;
    }
    
    public Map<String,String> getTitlesToAuthor(){
        Map<String,String> titleToAuthor = new HashMap<String,String>();
        for (String author : datamap.keySet()){
            for (String doctitle : datamap.get(author).keySet()){
                titleToAuthor.put(doctitle, author);
            }
        }
        return titleToAuthor;
    }
    
    /**
     * Loads a DataMap from a CSV file. Note that the CSV file must be dense.
     * The first row should consist solely of the datamap name
     * The second row should describe all of the columns. The first column must be authorName and the second must be the document title
     * @param path path to CSV file
     * @return initialized datamap
     */
    public DataMap loadDataMapFromCSV(String path){
        DataMap map = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(path)));
            String name = br.readLine();
            
            String featureline = br.readLine();
            
            String[] loadedFeatures = featureline.split(",");
            //skip i=0=authorName and i=1=document
            List<String> features = new ArrayList<String>(loadedFeatures.length-2);
            for (int i = 2; i<loadedFeatures.length; i++){
                if (!loadedFeatures[i].equalsIgnoreCase(""))
                    features.add(loadedFeatures[i]);
            }
            
            map = new DataMap(name,features);
            
            while (br.ready()){
                /* TODO this logic (and the write to CSV logic) needs to be completely redone. 
                String line = br.readLine();
                String[] parts = line.split(",");
                ConcurrentHashMap<Integer,FeatureData> docfeatures = new ConcurrentHashMap<Integer,FeatureData>();
                for (int i = 2; i<parts.length; i++){
                    
                    //docfeatures.put(i-2, Double.parseDouble(parts[i]));
                }
                
                if (!map.getDataMap().containsKey(parts[0]))
                    map.initAuthor(parts[0]);
                    
                Map<String,Integer> norms = new HashMap<String,Integer>();
                map.addDocumentData(parts[0], parts[1], norms, docfeatures);
                */
            }
            
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read in CSV file");
        }
        return map;
    }
    
}

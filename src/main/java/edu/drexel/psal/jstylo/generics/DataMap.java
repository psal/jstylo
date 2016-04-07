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

    //so... this is a multi-tiered map containing everything we need to know. 
    private ConcurrentHashMap<String, //outer layer is indexed on author identities
        ConcurrentHashMap<String, //middle layer is indexed on document titles
            ConcurrentHashMap<Integer, //inner layer is indexed on feature index
                Double>>> //data value is the double value for a given doc for a given author for that index feature
                    datamap;
    
    private Map<Integer,String> features;
    
    private String datasetName;
    
    public DataMap(String name, List<String> featureNames){
        datasetName = name;
        setFeatures(featureNames);
        datamap = new ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>>();
    }
    
    //add all authors prior to documents to ensure minimum amount of reads in threads. 
    public void initAuthor(String author){
        ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>> newAuthorMap = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>();
        datamap.put(author, newAuthorMap);
    }

    // add things
    public void addDocumentData(String author, String documentTitle, ConcurrentHashMap<Integer, Double> map) {
        datamap.get(author).put(documentTitle, map);
    }
    
    //returns things
    public ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>> getDataMap(){
        return datamap;
    }

    public Map<Integer,String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> featureList) {
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
    
    //TODO think it's safe to say that this is causing the issue.
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
                    ConcurrentHashMap<Integer,Double> docData = datamap.get(author).get(document);
                    //remove the index/feature if it is present
                    if (docData.containsKey(indexToRemove)){
                        docData.remove(indexToRemove);
                    }
                    //regardless, bump down the index of ALL other features
                    ConcurrentHashMap<Integer,Double> newDocData = new ConcurrentHashMap<Integer,Double>();
                    for (Integer key : docData.keySet()){
                        if (key < indexToRemove){ //if the index is under i, add as normal
                            newDocData.put(key, docData.get(key));
                        } else if (key > indexToRemove){ // if the index is over i, add it with a - 1
                            newDocData.put(key-1, docData.get(key));
                        }
                    }
                    datamap.get(author).put(document, newDocData);
                }
            }
                
            //adjust all future indices to remove to account for 
            for (int j = i+1; j < indicesToRemove.length; j++){
                if (indicesToRemove[i] <= indicesToRemove[j])
                    indicesToRemove[j] = indicesToRemove[j]-1;
            }
        }
        
        //TODO set the new map
    }

    
    public int numDocuments(){
        int count = 0;
        for (String author : datamap.keySet()){
            count+=datamap.get(author).size();
        }
        return count;
    }
    
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
                    ConcurrentHashMap<Integer,Double> docdata = datamap.get(author).get(document);
                    for (int i = 0; i <features.size(); i++){
                        if (docdata.containsKey(i))
                            nextline+=docdata.get(i)+",";
                        else
                            nextline+="0,";
                    }
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
                String line = br.readLine();
                String[] parts = line.split(",");
                ConcurrentHashMap<Integer,Double> docfeatures = new ConcurrentHashMap<Integer,Double>();
                for (int i = 2; i<parts.length; i++){
                    if (!parts[i].equalsIgnoreCase(""))
                        docfeatures.put(i-2, Double.parseDouble(parts[i]));
                }
                map.addDocumentData(parts[0], parts[1], docfeatures);
            }
            
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read in CSV file");
        }
        return map;
    }
    
}

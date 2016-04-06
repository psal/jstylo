package edu.drexel.psal.jstylo.generics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataMap {

    //so... this is a multi-tiered map containing everything we need to know. 
    private ConcurrentHashMap<String, //outer layer is indexed on author identities
        ConcurrentHashMap<String, //middle layer is indexed on document titles
            ConcurrentHashMap<Integer, //inner layer is indexed on feature index
                Double>>> //data value is the double value for a given doc for a given author for that index feature
                    datamap;
    
    private Map<Integer,String> features;
    
    @SuppressWarnings("unused")
    private String datasetName;
    
    public DataMap(String name, List<String> featureNames){
        datasetName = name;
        setFeatures(featureNames);
        datamap = new ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>>();
    }
    
    //add things
    public void addDocumentData(String author, String documentTitle ,ConcurrentHashMap<Integer,Double> map){
        if (datamap.containsKey(author)){
            datamap.get(author).put(documentTitle, map);
        } else {
            ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>> newAuthorMap = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>();
            newAuthorMap.put(documentTitle, map);
            datamap.put(author, newAuthorMap);
        }
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
    
}

package edu.drexel.psal.jstylo.generics;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DataMap {

    //so... this is a multi-tiered map containing everything we need to know. 
    private ConcurrentHashMap<String, //outer layer is indexed on author identities
        ConcurrentHashMap<String, //middle layer is indexed on document titles
            ConcurrentHashMap<Integer, //inner layer is indexed on feature index
                Double>>> //data value is the double value for a given doc for a given author for that index feature
                    datamap;
    
    private List<String> features;
    
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
    
    //remove things
    //TODO
    
    
    //returns things
    public ConcurrentHashMap<String,ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>>> getDataMap(){
        return datamap;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }
    
    public int numDocuments(){
        int count = 0;
        for (String author : datamap.keySet()){
            count+=datamap.get(author).size();
        }
        return count;
    }
    
}

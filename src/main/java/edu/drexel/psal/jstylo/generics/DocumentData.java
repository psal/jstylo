package edu.drexel.psal.jstylo.generics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentData {

    private Map<String,Integer> normalizationValues;
    private ConcurrentHashMap<Integer,FeatureData> dataValues;

    public DocumentData(Map<String,Integer> norms, ConcurrentHashMap<Integer,FeatureData> data){
        normalizationValues = norms;
        dataValues = data;
    }
    
    public Map<String,Integer> getNormalizationValues(){
        return normalizationValues;
    }
    
    public ConcurrentHashMap<Integer,FeatureData> getDataValues(){
        return dataValues;
    }
    
    public Integer getFeatureCountAtIndex(int i){
        if (dataValues.containsKey(i)){
            return dataValues.get(i).getCount();
        } else {
            return -1;
        }
    }
    
    public void replaceFeatureValues(ConcurrentHashMap<Integer,FeatureData> newmap){
        dataValues = newmap;
    }
    
}

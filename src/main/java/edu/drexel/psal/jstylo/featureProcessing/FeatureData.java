package edu.drexel.psal.jstylo.featureProcessing;

public class FeatureData {

    private String name;
    private String normalizationType;
    private Integer count;
    private Double value;
    
    public FeatureData(String name, String normalization, Integer count){
        this.name = name;
        this.count = count;
        normalizationType = normalization;
    }
    
    public void setValue(Double value){
        this.value = value;
    }
    
    public String getName(){
        return name;
    }

    public String getNormalizationType(){
        return normalizationType;
    }
    
    public Integer getCount(){
        return count;
    }
    
    public Double getValue(){
        return value;
    }
}

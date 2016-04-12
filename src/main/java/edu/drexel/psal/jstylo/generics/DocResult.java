package edu.drexel.psal.jstylo.generics;

import java.util.Map;

import com.google.gson.JsonObject;

/**
 * This class contains the results of processing for a single document.
 * If a document has an unknown author, its actual author field would be 'unknown' 
 * 
 * @author Travis Dutko
 */
public class DocResult {
    
    private String title;
    private String actualAuthor;
    private String suspectedAuthor;
    private Map<String,Double> probabilityMap;
    private final String defaultUnknown = "_Unknown_";
    
    public DocResult(String title, String actual){
        this.title = title;
        actualAuthor = actual;
    }
    
    public DocResult(String title){
        this.title = title;
        actualAuthor = defaultUnknown;
    }
    
    public DocResult(String title, String actual, Map<String,Double> probabilities){
        this.title = title;
        actualAuthor = actual;
        assignProbabilitiesAndDesignateSuspect(probabilities);
    }
    
    public DocResult(String title, Map<String,Double> probabilities){
        this.title = title;
        actualAuthor = defaultUnknown;
        assignProbabilitiesAndDesignateSuspect(probabilities);
    }
    
    /**
     * Use this OR assignProbabilitiesAndDesignateSuspect
     * @param suspect
     */
    public void designateSuspect(String suspect){
        suspectedAuthor = suspect;
        probabilityMap.clear();
        probabilityMap = null;
    }
    
    /**
     * Use this OR designateSuspect
     * @param probabilities
     */
    public void assignProbabilitiesAndDesignateSuspect(Map<String,Double> probabilities){
        probabilityMap = probabilities;
        String topSuspect = "";
        Double topProbability = 0.0;
        for (String suspect : probabilities.keySet()){
            if (probabilities.get(suspect) > topProbability){
                topProbability = probabilities.get(suspect);
                topSuspect = suspect;
            }
        }
        suspectedAuthor = topSuspect;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getActualAuthor(){
        return actualAuthor;
    }
    
    public String getSuspectedAuthor(){
        return suspectedAuthor;
    }
    
    public Map<String,Double> getProbabilities(){
        return probabilityMap;
    }
    
        public JsonObject toJson(){
    	
    	JsonObject docResultJson = new JsonObject();
    	docResultJson.addProperty("title", title);
    	docResultJson.addProperty("actualAuthor", actualAuthor);
    	
    	JsonArray probabilityMapJsonArray = new JsonArray();
    	
    	
    	for(String key : probabilityMap.keySet()){
    		JsonObject tempJsonObject = new JsonObject();
    		tempJsonObject.addProperty(key, probabilityMap.get(key));
    		probabilityMapJsonArray.add(tempJsonObject);
    	}
  
    	docResultJson.add("probabilityMap", probabilityMapJsonArray);
    	
    	return docResultJson;
    }
    
    @Override
    public String toString(){
        return String.format("For document %s, the true author is %s. After analysis, our top suspect is %s with a %.2f likelihood\n",
                title,
                actualAuthor,
                suspectedAuthor,
                probabilityMap.get(suspectedAuthor));
    }
    
}

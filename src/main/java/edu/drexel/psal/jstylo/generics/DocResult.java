package edu.drexel.psal.jstylo.generics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class contains the results of processing for a single document.
 * If a document has an unknown author, its actual author field would be 'unknown' 
 * 
 * @author Travis Dutko
 */
public class DocResult implements Serializable{
    
    private static final long serialVersionUID = 1L;
    private String title;
    private String actualAuthor;
    private String suspectedAuthor;
    private Map<String,Double> probabilityMap;
    private List<String> potentialAuthors;
    public static final String defaultUnknown = "_Unknown_";
    
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
    public void designateSuspect(String suspect, List<String> suspectSet){
        suspectedAuthor = suspect;
        if (probabilityMap != null){
            probabilityMap.clear();
            probabilityMap = null;
        }
        potentialAuthors = suspectSet;
        
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
        if (probabilityMap != null)
            return probabilityMap;
        else {
            Map<String,Double> probs = new HashMap<String,Double>();
            for (String suspect : potentialAuthors){
                if (suspect.equals(suspectedAuthor))
                    probs.put(suspect, 1.0);
                else
                    probs.put(suspect, 0.0);
            }
            return probs;
        }
    }
    

    public JsonObject toJson() {

        JsonObject docResultJson = new JsonObject();
        docResultJson.addProperty("title", title);
        docResultJson.addProperty("actualAuthor", actualAuthor);

        JsonArray probabilityMapJsonArray = new JsonArray();

        if (probabilityMap != null) {
            for (String key : probabilityMap.keySet()) {
                JsonObject tempJsonObject = new JsonObject();
                tempJsonObject.addProperty("Author", key);
                tempJsonObject.addProperty("Probability", probabilityMap.get(key));
                probabilityMapJsonArray.add(tempJsonObject);
            }
        } else {
            docResultJson.addProperty("suspectedAuthor", suspectedAuthor);
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

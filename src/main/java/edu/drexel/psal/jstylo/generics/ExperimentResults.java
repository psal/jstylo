package edu.drexel.psal.jstylo.generics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class is a post-processing data holding class.
 * It contains all of the results on a per-document basis and computes some basic statistics.
 * 
 * For the future, consider making this class extensible and having each ML extending it to add their own statistics.
 * Maybe make it crossvalidation/fold aware for finer granularity. 
 * 
 * @author Travis Dutko
 */
public class ExperimentResults implements Serializable{
    
    private static final long serialVersionUID = 1L;
    private List<DocResult> experimentContents;
    private List<String> authorList; //for use with confusion matrix
    
    public ExperimentResults(){
        experimentContents = new ArrayList<DocResult>();
    }

    public List<DocResult> getExperimentContents() {
        return experimentContents;
    }

    public void addDocResult(DocResult dr){
        experimentContents.add(dr);
    }
    
    public double getTruePositiveRate(){
        double correct = 0.0;
        double incorrect = 0.0;
        
        for (DocResult result : experimentContents){
            if (result.getActualAuthor().equalsIgnoreCase(result.getSuspectedAuthor()))
                correct += 1.0;
            else 
                incorrect += 1.0;
        }

        return correct/(correct+incorrect);
    }
    
    public int getCorrectDocCount(){
        int correct = 0;
        for (DocResult result : experimentContents){
            if (result.getActualAuthor().equalsIgnoreCase(result.getSuspectedAuthor()))
                correct += 1.0;
        }
        return correct;
    }
    
    //expand this as we add more relevant stats to the processing
    public String getStatisticsString(){
        String statString = "";
        statString+=String.format("Correctly Identified %d out of %d documents\n", getCorrectDocCount(),experimentContents.size());
        statString+="\n((Calculated Statistics))\n";
        statString+=String.format("True Positive Percentage: %.2f\n",getTruePositiveRate()*100);
        return statString;
    }
    
    public String getSimpleResults(){
        String results = "((Simple Document Results))\n";
        results += String.format("%-14s | %-14s |\n","Document Title","Top Suspect");
        for (DocResult result : experimentContents){
            
            results+=String.format("%-14s | %-14s |\n", 
                    result.getTitle(),result.getSuspectedAuthor());
            
        }
        return results;
    }
    
    public String getAllDocumentResults(boolean known){
        
        String results = "((Individual Document Results))\n";
        results += String.format("%-14s | %-14s | %-14s | %-14s |\n","Document Title","Actual","Top Suspect","Probability");
        for (DocResult result : experimentContents){
            String probString = String.format("%.2f", result.getProbabilities().get(result.getSuspectedAuthor()));
            
            results+=String.format("%-14s | %-14s | %-14s | %-14s |", 
                    result.getTitle(),result.getActualAuthor(),result.getSuspectedAuthor(),probString);
            
            if (result.getActualAuthor().equals(result.getSuspectedAuthor()) && known)
                results += " Correct!\n";
            else if (known)
                results +=" Incorrect...\n";
            else if (!known)
                results+="\n";
        }
        return results;
    }
    
    
    public String getAllDocumentResultsVerbose(){
        
        String results = "((Verbose Document Results))\n";
        results += "Document Title |";
        
        //first column has all of the author names
        for (String author : experimentContents.get(0).getProbabilities().keySet()){
            results+=String.format(" %-10s |", author);
        }
        
        results+="\n";
        
        for (DocResult result : experimentContents){
            results+=String.format("%-14s |", result.getTitle());
            
            Double topProbability = -1.0;
            String topAuthor = "unidentified";
            
            for (String author : result.getProbabilities().keySet()){
                if (result.getProbabilities().get(author) > topProbability){
                    topProbability = result.getProbabilities().get(author);
                    topAuthor = author;
                }
            }
            
            for (String author : result.getProbabilities().keySet()){
                if (topAuthor.equals(author)){
                    results+=String.format("  >>%.2f<<  |", result.getProbabilities().get(author));
                } else {
                    results+=String.format("    %.2f    |", result.getProbabilities().get(author));
                }
            }
            results+="\n";
        }
        return results;
    }
    
    public JsonObject toJson(){
<<<<<<< HEAD
    	
    	JsonObject experimentContentsJson = new JsonObject();
    	
    	JsonArray experimentContentsJsonArray = new JsonArray();
    	
    	for(DocResult docResult : experimentContents){
    		experimentContentsJsonArray.add(docResult.toJson());
    	}
    	experimentContentsJson.add("experimentContents", experimentContentsJsonArray);

        return experimentContentsJson;
=======
        
        JsonObject experimentContentsJson = new JsonObject();
        
        JsonArray experimentContentsJsonArray = new JsonArray();
        
        for(DocResult docResult : experimentContents){
            experimentContentsJsonArray.add(docResult.toJson());
        }
        experimentContentsJson.add("experimentContents", experimentContentsJsonArray);

        return experimentContentsJson;
    }
    
    //x axis is who we labeled the author as
    //y axis is who the author actually is
    public int[][] getConfusionMatrix(){
        
        authorList = new ArrayList<String>();
        for (String author : experimentContents.get(0).getProbabilities().keySet()){
            authorList.add(author);
        }
        Collections.sort(authorList);
        
        //initialize the matrix
        int[][] matrix = new int[authorList.size()][authorList.size()];
        for (int i = 0; i < matrix.length; i++){
            for (int j=0; j<matrix[i].length; j++){
                matrix[i][j] = 0;
            }
        }
        
        //increment all values
        for (DocResult dr : experimentContents){
            matrix[authorList.indexOf(dr.getActualAuthor())][authorList.indexOf(dr.getSuspectedAuthor())]+=1;
        }
        
        return matrix;
>>>>>>> refs/remotes/tdutko/master
    }
    
    public String getConfusionMatrixString(){
        int space = 16*experimentContents.get(0).getProbabilities().keySet().size()+2;
        String confusionMatrix = String.format("%-"+space+"s|", "Suspected Authors");
        confusionMatrix+=String.format("|||| %14s|\n","Actual Authors");
        //add each author to the top
        for (String author : experimentContents.get(0).getProbabilities().keySet()){
            confusionMatrix+=String.format(" %-14s |",author);
        }
        confusionMatrix+="||||\n";
        for (int i =0; i<space+20; i++){
            confusionMatrix+="_";
        }
        confusionMatrix+="\n";
        
        int[][] matrix = getConfusionMatrix();
        
        for (int i = 0; i < matrix.length; i++){
            for (int j=0; j<matrix[i].length; j++){
                confusionMatrix+=String.format(" %-14d |", matrix[i][j]);
            }
            confusionMatrix+=String.format("|||| %14s|\n",authorList.get(i));
        }
        
        return confusionMatrix;
    }
}

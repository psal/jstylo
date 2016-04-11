package edu.drexel.psal.jstylo.generics;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a post-processing data holding class.
 * It contains all of the results on a per-document basis and computes some basic statistics.
 * 
 * For the future, consider making this class extensible and having each ML extending it to add their own statistics.
 * Maybe make it crossvalidation/fold aware for finer granularity. 
 * 
 * @author Travis Dutko
 */
public class ExperimentResults {
    
    private List<DocResult> experimentContents;
    
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
    
    //expand this as we add more relevant stats to the processing
    public String getStatisticsString(){
        String statString = "";
        statString+=String.format("Correctly classified %.2f percent of all documents\n",getTruePositiveRate()*100);
        return statString;
    }
    
    //TODO switch on/off actual column based on if actual is known
    //TODO add some sort of quickly visible "correct!" mark or column
    public String getAllDocumentResults(){
        String results = String.format("%-14s | %-14s | %-14s | %-14s |\n","Document Title","Actual","Suspect","Probability");
        for (DocResult result : experimentContents){
            results+=String.format("%-14s | %-14s | %-14s | %-14s |\n", 
                    result.getTitle(),result.getActualAuthor(),result.getSuspectedAuthor(),
                        String.format("%.2f", result.getProbabilities().get(result.getSuspectedAuthor())));
        }
        return results;
    }
    
    public String getAllDocumentResultsVerbose(){
        String results = "Document Title |";
        
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
    
    //TODO
    public String getConfusionMatrix(){
        String confusionMatrix = "";
        
        
        return confusionMatrix;
    }
}

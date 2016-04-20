package edu.drexel.psal.jstylo.machineLearning.weka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.drexel.psal.jstylo.featureProcessing.FeatureData;
import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocResult;
import edu.drexel.psal.jstylo.generics.DocumentData;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Provides a couple of static compatability methods between JStylo and Weka. 
 * These methods can only be accessed within this package.
 * There should not be any reason to use them external to this package.
 * 
 * @author Travis Dutko
 */
public class WekaUtils {
    
    public static Instances instancesFromDataMap(DataMap datamap){
        Instances instances = null;
        FastVector attributes = createFastVector(datamap.getFeatures(),datamap.getDataMap().keySet());
        
        int numfeatures = attributes.size();
        instances = new Instances("Instances",attributes,datamap.numDocuments());
        //for each author...
        for (String author : datamap.getDataMap().keySet()){
            ConcurrentHashMap<String,DocumentData> authormap 
                = datamap.getDataMap().get(author);
            
            //for each document...
            for (String doctitle : authormap.keySet()){
                
                Instance instance = new SparseInstance(numfeatures);
                ConcurrentHashMap<Integer,FeatureData> documentData = authormap.get(doctitle).getDataValues();
                
                //for each index we have a value for 
                for (Integer index : documentData.keySet()){
                    instance.setValue((Attribute)attributes.elementAt(index), documentData.get(index).getValue());
                }
                instance.setValue((Attribute)attributes.elementAt(attributes.size()-1), author);
                instances.add(instance);
            }
        }
        
        return instances;
    }
    
    protected static FastVector createFastVector(Map<Integer,String> features, Set<String> authors){
        FastVector fv = new FastVector(features.size()+1);
        
        for (Integer i : features.keySet()){
            fv.addElement(new Attribute(features.get(i),i));
        }
        
        //author names
        FastVector authorNames = new FastVector();
        List<String> authorsSorted = new ArrayList<String>(authors.size());
        authorsSorted.addAll(authors);
        
        for (String author : authorsSorted){
            authorNames.addElement(author);
        }
        Attribute authorNameAttribute = new Attribute("authorName", authorNames);
        
        fv.addElement(authorNameAttribute);
        
        return fv;
    }
    
    protected static ExperimentResults resultsFromEvaluation(Evaluation eval, String authorCSV, List<String> documentTitles){
        ExperimentResults results = new ExperimentResults();
        
        FastVector predictions = eval.predictions();
        String[] authors  = getAuthorsFromAttributeString(authorCSV);
        
        //for each document
        for (int i = 0; i<predictions.size(); i++){
            NominalPrediction prediction = (NominalPrediction)predictions.elementAt(i);
            String actual = authors[(int)prediction.actual()];
            double[] probabilities = prediction.distribution();
            
            Map<String,Double> probMap = new HashMap<String,Double>();
            
            //for each potential author...
            for (int j = 0; j< probabilities.length; j++){
                probMap.put(authors[j], probabilities[j]);
            }
            results.addDocResult(new DocResult(documentTitles.get(i),actual,probMap));
        }
        
        return results;
    }
    
    private static String[] getAuthorsFromAttributeString(String authorCSV){
        authorCSV = authorCSV.substring(authorCSV.indexOf('{')+1, authorCSV.indexOf('}'));
        return authorCSV.split(",");
    }
    
}

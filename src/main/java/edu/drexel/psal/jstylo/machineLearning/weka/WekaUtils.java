package edu.drexel.psal.jstylo.machineLearning.weka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.drexel.psal.jstylo.generics.DataMap;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class WekaUtils {
    protected static Instances instancesFromDataMap(DataMap datamap){
        Instances instances = null;
        FastVector attributes = createFastVector(datamap.getFeatures(),datamap.getDataMap().keySet());
        
        int numfeatures = attributes.size();
        instances = new Instances("Instances",attributes,datamap.numDocuments());
        
        //for each author...
        for (String author : datamap.getDataMap().keySet()){
            ConcurrentHashMap<String,ConcurrentHashMap<Integer,Double>> authormap 
                = datamap.getDataMap().get(author);
            
            //for each document...
            for (String doctitle : authormap.keySet()){
                
                Instance instance = new SparseInstance(numfeatures);
                ConcurrentHashMap<Integer,Double> documentData = authormap.get(doctitle);
                
                //for each index we have a value for 
                for (Integer index : documentData.keySet()){
                    instance.setValue((Attribute)attributes.elementAt(index), documentData.get(index));
                }
                instance.setValue((Attribute)attributes.elementAt(attributes.size()-1), author);
                instances.add(instance);
            }
        }
        
        return instances;
    }
    
    protected static DataMap datamapFromInstances(Instances instances,boolean hasTitle){
        
        //get all feature labels
        List<String> features = new ArrayList<String> (instances.numAttributes());
        for (int i = 0; i<instances.numAttributes(); i++){
            if (!instances.attribute(i).name().equalsIgnoreCase("authorName"))
                features.add(i, instances.attribute(i).name());
        }
        
        DataMap datamap = new DataMap("developingMap",features);
        
        for (int i = 0; i<instances.numInstances(); i++){
            Instance document = instances.instance(i);
            
            String author = document.stringValue(document.numAttributes()-1);
            String doctitle = "post-processed";
            if (hasTitle)
                doctitle = document.stringValue(0);
            
            ConcurrentHashMap<Integer,Double> docMap = new ConcurrentHashMap<Integer,Double>();
            for (int j = 1; j<document.numAttributes()-1; j++){
                
                //shifting left 1 for generics sake--most ML libraries don't use the 0th as doc title.
                docMap.put(j-1, document.value(j));
            }

            datamap.addDocumentData(author, doctitle, docMap);
        }

        return datamap;
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
}

package edu.drexel.psal.jstylo.generics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import weka.core.Instance;
import weka.core.Instances;

public class Temp {

    public static DataMap datamapFromInstances(Instances instances,boolean hasTitle){
        
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
    
}

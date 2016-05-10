package edu.drexel.psal.jstylo.machineLearning.weka;

import java.util.Arrays;
import java.util.Comparator;

import edu.drexel.psal.jstylo.generics.DataMap;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * A class with static methods to compute and apply infogain statistics to a DataMap. Located in this project as InfoGain is a Weka measurement of usefulness.
 * Note that due to it using DataMaps, this class can be used with any machine learning / feature processing set. 
 * 
 * Note that if we want to apply infogain on an instances object we can still do so
 *      by applying InfoGain before converting to an Instances object to begin with or translating back and forth.
 * 
 * @author Travis Dutko
 */
public class InfoGain {

    /**
     * Calculates InfoGain on the instances to provide information on how useful each feature was to identifying the documents.<br>
     * @param data the datamap to analyze
     * @return a two-dimensional sorted array with one element per feature. It is sorted via how useful each feature was and stores the index.
     * @throws Exception
     */
    public static double[][] calcInfoGain(DataMap data) throws Exception {

        Instances insts = WekaUtils.instancesFromDataMap(data);
        //initialize values
        int len = 0;
        int n = insts.numAttributes();
        Attribute classAttribute = insts.attribute(insts.numAttributes() - 1);
        InfoGainAttributeEval ig = new InfoGainAttributeEval();
        insts.setClass(classAttribute);
        ig.buildEvaluator(insts);

        // extract and sort attributes by InfoGain
        double[][] infoArr = new double[n - 1][2];
        int j = 0;
        for (int i = 0; i < infoArr.length; i++) {
            if (insts.attribute(j).name().equals("authorName")) {
                ;
            } else {
                len = (len > insts.attribute(j).name().length() ? len : insts
                        .attribute(j).name().length());
                infoArr[i][0] = ig.evaluateAttribute(j);
                infoArr[i][1] = j;
            }
            j++;
        }
        //sort based on usefulness
        Arrays.sort(infoArr, new Comparator<double[]>() {
            @Override
            public int compare(final double[] first, final double[] second) {
                return -1 * ((Double) first[0]).compareTo(((Double) second[0]));
            }
        });

        return infoArr;
    }

    /**
     * Removes all but the top N features (as returned by calcInfoGain) from the datamap object.<br>
     * @param the indices and infoGain values of all attributes
     * @param data the datamap to remove infoGain from
     * @param n the number of features to keep
     * @throws Exception
     */
    public static double[][] applyInfoGain(double[][] sortedFeatures, DataMap data, int n)
            throws Exception {
        
        //find out how many values to remove
        int valuesToRemove =-1;
        if (n>sortedFeatures.length)
            return sortedFeatures;
        else
            valuesToRemove = sortedFeatures.length-n;
        
        double[][] keepArray = new double[n][2]; //array to be returned
        double[][] removeArray = new double[valuesToRemove][2]; //array to be sorted and be removed from the Instances object
        
        //populate the arrays
        for (int i =0; i<sortedFeatures.length; i++){
            if (i<n){
                keepArray[i][0] = sortedFeatures[i][0];
                keepArray[i][1] = sortedFeatures[i][1];
            } else {
                removeArray[i-n][0] = sortedFeatures[i][0];
                removeArray[i-n][1] = sortedFeatures[i][1];
            }
        }
        
        //sort based on index
        Arrays.sort(removeArray, new Comparator<double[]>() {
            @Override
            public int compare(final double[] first, final double[] second) {
                return -1 * ((Double) first[1]).compareTo(((Double) second[1]));
            }
        });
        
        Integer[] toRemoveIndices = new Integer[removeArray.length];
        for (int i = 0; i<removeArray.length; i++){
            toRemoveIndices[i] = (int) Math.round(removeArray[i][1]); //FIXME needs to be a better way than string convert. Not hardcasting as that'll error out.
        }
        
        data.removeFeatures(toRemoveIndices);
        
        //return the array consisting only of the top n values
        return keepArray;
    }
    
}

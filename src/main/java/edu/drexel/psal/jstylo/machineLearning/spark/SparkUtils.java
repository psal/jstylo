package edu.drexel.psal.jstylo.machineLearning.spark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.FeatureData;

public class SparkUtils {

    /**
     * Converts the DataMap to JavaRDD<LabeledPoint> which is what most Spark mllib classes need to perform classification.
     * @param sql
     * @param map
     * @param labels
     * @return
     */
    public static JavaRDD<LabeledPoint> DataFrameToLabeledPointRDD(DataFrame df){
        return df.javaRDD().map(new LabeledFromRow());
    }
    
    public static DataFrame DataMapToDataFrame(SQLContext sql,DataMap map,Map<String,Double> labels){
        return sql.createDataFrame(transformDataMap(map,labels), LabeledPoint.class);
    }
    
    /**
     * The labels (authors) that each double corresponds to (since spark needs double labels, for some reason)
     * @param map
     * @return
     */
    public static Map<String,Double> getLabelMap(DataMap map){
        Map<String,Double> labelMap = new HashMap<String,Double>();
        Double label = 0.0;
        for (String author : map.getDataMap().keySet()){
            labelMap.put(author, label);
            label+=1;
        }
        return labelMap;
    }
    
    /**
     * Spark function to parse the data in a parallelized fashion
     */
    private static class LabeledFromRow implements Function<Row,LabeledPoint>{
        
        private static final long serialVersionUID = 1L;
        
        @Override
        public LabeledPoint call(Row arg0) throws Exception {
            return new LabeledPoint(arg0.getDouble(1),arg0.getAs(0));
        }
    }
    
    
    /**
     * Converts a datamap into a list of labeled points to be converted into a dataframe
     */
    private static List<LabeledPoint> transformDataMap(DataMap map,Map<String,Double> labels){
        List<LabeledPoint> points = new ArrayList<LabeledPoint>();
        for (String author : map.getDataMap().keySet()){
            for (String document : map.getDataMap().get(author).keySet()){
                points.add(new LabeledPoint(labels.get(author),
                        getVector(map.getDataMap().get(author).get(document).getDataValues(),map.getFeatures().keySet().size())));
            }
        }
        return points;
    }
    
    /**
     * converts a datamap's doc data into a vector object to be used in the creation of a LabeledPoint object
     * @param docdata
     * @param numfeatures
     * @return
     */
    private static Vector getVector(Map<Integer,FeatureData> docdata, int numfeatures) {
        List<Integer> keys = new ArrayList<Integer>(docdata.keySet().size());
        List<Double> values = new ArrayList<Double>(docdata.keySet().size());
        for (Integer i : docdata.keySet()){
            keys.add(i);
            values.add(docdata.get(i).getValue());
        }
        
        return Vectors.sparse(numfeatures, Ints.toArray(keys), Doubles.toArray(values));
    }
}

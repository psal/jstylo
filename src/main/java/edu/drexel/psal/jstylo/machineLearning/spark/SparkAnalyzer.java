package edu.drexel.psal.jstylo.machineLearning.spark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgaap.generics.Document;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocResult;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import scala.Tuple2;

public class SparkAnalyzer extends Analyzer implements Serializable{

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SparkAnalyzer.class);
    private List<String> documentTitles;
    private List<String> suspectSet;
    private Map<String,String> titlesAndAuthors;
    
    //eventually have this initialize the default classifier, then add a constructor which allows for alternative classifiers
    //just an empty constructor is fine for now, though
    //TODO
    public SparkAnalyzer (){}

    @Override
    public ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed) {
        //initialize context/data
        JavaSparkContext context = new JavaSparkContext(
                new SparkConf().setAppName("Spark Classifier").setMaster("local[*]"));
        SQLContext sql = new SQLContext(context);
        Map<String,Double> labels = SparkUtils.getLabelMap(data);
        documentTitles = null;
        titlesAndAuthors = null;
        initSuspects(data);
        DataFrame df = SparkUtils.DataMapToDataFrame(sql, data, labels);
        
        //initialize return object
        ExperimentResults er = new ExperimentResults();
        
        //builds fold array
        double[] splitArray = new double[folds];
        for (int i =0; i<splitArray.length; i++)
            splitArray[i] = 1.0/folds;
        
        //splits the data into equal folds
        DataFrame[] splits = df.randomSplit(splitArray,randSeed);
        for (int i = 0; i<folds; i++){
            //run analysis call classifyWithKnown authors using one split as testing and the rest as training
            DataFrame test = splits[i];
            DataFrame train = df.except(test);
            ExperimentResults foldIResults = classify(sql,labels,train,test);
            
            //amalgamate results
            for (DocResult dr : foldIResults.getExperimentContents()){
                er.addDocResult(dr);
            }
        }
        
        context.close();
        return er;
    }

    @Override
    public ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed, int relaxFactor) {
        LOG.warn("Spark Analyzer does not yet support relaxed cross validation. Running normal cross validation instead.");
        return runCrossValidation(data,folds,randSeed);
    }
    
    @Override
    public ExperimentResults classifyWithUnknownAuthors(DataMap train, DataMap test, List<Document> unknownDocs) {
        //initialize data
        JavaSparkContext context = new JavaSparkContext(
                new SparkConf().setAppName("Spark Classifier").setMaster("local[*]"));
        SQLContext sql = new SQLContext(context);
        Map<String,Double> labels = SparkUtils.getLabelMap(train);
        documentTitles = test.getDocumentTitles();
        initSuspects(train);
        suspectSet.add(DocResult.defaultUnknown);
        ExperimentResults results = classify(sql,labels,SparkUtils.DataMapToDataFrame(sql, train, labels),SparkUtils.DataMapToDataFrame(sql, test, labels));
        context.close();
        return results;
    }
    
    @Override
    public ExperimentResults classifyWithKnownAuthors(DataMap train, DataMap test) throws Exception {
        
        //initialize data
        JavaSparkContext context = new JavaSparkContext(
                new SparkConf().setAppName("Spark Classifier").setMaster("local[*]"));
        SQLContext sql = new SQLContext(context);
        Map<String,Double> labels = SparkUtils.getLabelMap(train);
        documentTitles = test.getDocumentTitles();
        titlesAndAuthors = test.getTitlesToAuthor();
        initSuspects(train);
        ExperimentResults results = classify(sql,labels,SparkUtils.DataMapToDataFrame(sql, train, labels),SparkUtils.DataMapToDataFrame(sql, test, labels));
        context.close();
        return results;
    }

    //pulled this out of classifyWithKnownAuthors so it can be used for cross-validation
    private ExperimentResults classify(SQLContext sql, Map<String,Double> labels, DataFrame train, DataFrame test){
        //convert DF to RDD
        JavaRDD<LabeledPoint> trainingRDD = SparkUtils.DataFrameToLabeledPointRDD(train);
        JavaRDD<LabeledPoint> testingRDD = SparkUtils.DataFrameToLabeledPointRDD(test);
        
        //train classifier
        //eventually, replace this with a classifier determined dynamically
        //TODO
        LogisticRegressionWithLBFGS model = new LogisticRegressionWithLBFGS();
        model.setNumClasses(Integer.parseInt(""+labels.keySet().size()));
        LogisticRegressionModel classifier = model.run(trainingRDD.rdd());
        //probably still need a "classifier = model.run(trainingRDD.rdd());

        //ugly way of determining the true authors for this set of docs
        List<String> actualAuthors = new ArrayList<String>();
        for (LabeledPoint point : trainingRDD.collect()){
            for (String author : labels.keySet()){
                if (labels.get(author).equals(point.label())){
                    actualAuthors.add(author);
                    break;
                }
            }
        }
        
        // Compute raw scores on the test set.
        JavaRDD<Tuple2<Double, Double>> scoreAndLabels = testingRDD.map(
          new Function<LabeledPoint, Tuple2<Double, Double>>() {
            private static final long serialVersionUID = 1L;

            public Tuple2<Double, Double> call(LabeledPoint p) {
              Double predicted = classifier.predict(p.features());
              return new Tuple2<Double, Double>(predicted, p.label());
            }
          }
        );

        //convert results into experimentResults
        ExperimentResults er = new ExperimentResults();
        List<Tuple2<Double,Double>> results = scoreAndLabels.collect();
        
        //iterate over the results
        int index = 0;
        for (Tuple2<Double,Double> result : results){
            
            //if we have doctitles (for non-cross-validation), grab the title
            String title = "No Title";
            if (documentTitles != null){
                title = documentTitles.get(index);
            }
            DocResult dr = null;
            
            //if we know the authors, grab the true author for the title
            if (titlesAndAuthors == null){
                for (String author : labels.keySet()){
                    if (labels.get(author).equals(result._2)){
                        dr = new DocResult(title,author);
                        break;
                    }
                }
                //dr = new DocResult(title,actualAuthors.get(index));
            } else {
                dr = new DocResult(title,titlesAndAuthors.get(title));
            }

            for (String author : labels.keySet()) {
                if (labels.get(author).equals(result._1)) {
                    dr.designateSuspect(author, suspectSet);
                    break;
                }
            }

            er.addDocResult(dr);
            index++;
        }
        return er;
    }
    
    @Override
    public String[] optionsDescription() {
        String[] options = {"No options at this time"};
        return options;
    }

    @Override
    public String analyzerDescription() {
        return "A Machine Learning Analyzer which weighs the power of Apache Spark to perform data analysis";
    }

    @Override
    public String getName() {
        return "Spark Analyzer";
    }
    
    @Override
    public String getExperimentMetrics(){
        return "No metrics calculated";
    }

    public void initSuspects(DataMap data){
        suspectSet = new ArrayList<String>();
        for (String author : data.getTitlesToAuthor().values()){
            if (!suspectSet.contains(author))
                suspectSet.add(author);
        }
    }
    
}

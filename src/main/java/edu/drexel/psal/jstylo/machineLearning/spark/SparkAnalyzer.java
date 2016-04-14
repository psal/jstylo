package edu.drexel.psal.jstylo.machineLearning.spark;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.SQLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgaap.generics.Document;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import scala.Tuple2;

public class SparkAnalyzer extends Analyzer implements Serializable{

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(SparkAnalyzer.class);
    
    //TODO eventually have this initialize the default classifier, then add a constructor which allows for alternative classifiers
    public SparkAnalyzer (){
        
    }
    
    @Override
    public ExperimentResults classifyWithUnknownAuthors(DataMap trainingSet, DataMap testSet, List<Document> unknownDocs) {
        //TODO
        return null;
    }

    @Override
    public ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed) {
        //TODO finish this
        LOG.info("In Cross-validation");
        System.exit(2);
        JavaSparkContext context = new JavaSparkContext(
                new SparkConf().setAppName("Spark Classifier").setMaster("local[*]"));
        SQLContext sql = new SQLContext(context);
        Map<String,Double> labels = SparkUtils.getLabelMap(data);
        JavaRDD<LabeledPoint> rdd = SparkUtils.DataMapToLabeledPointRDD(sql, data, labels);
        LOG.info("Data converted: "+rdd.count());
        
        return null;
    }

    @Override
    public ExperimentResults runCrossValidation(DataMap data, int folds, long randSeed, int relaxFactor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExperimentResults classifyWithKnownAuthors(DataMap train, DataMap test) throws Exception {
        // TODO Auto-generated method stub
        LOG.info("In classify");
        //initialize data
        JavaSparkContext context = new JavaSparkContext(
                new SparkConf().setAppName("Spark Classifier").setMaster("local[*]"));
        SQLContext sql = new SQLContext(context);
        Map<String,Double> labels = SparkUtils.getLabelMap(train);
        JavaRDD<LabeledPoint> trainingRDD = SparkUtils.DataMapToLabeledPointRDD(sql, train, labels);
        JavaRDD<LabeledPoint> testingRDD = SparkUtils.DataMapToLabeledPointRDD(sql, test, labels);
        
        //train classifier
        LogisticRegressionWithLBFGS model = new LogisticRegressionWithLBFGS();
        model.setNumClasses(Integer.parseInt(""+labels.keySet().size()));
        LogisticRegressionModel classifier = model.run(trainingRDD.rdd());
        
        LOG.info("trainingsize: "+trainingRDD.count()+" testingsize: "+testingRDD.count());
        
        // Compute raw scores on the test set.
        JavaRDD<Tuple2<Object, Object>> scoreAndLabels = testingRDD.map(
          new Function<LabeledPoint, Tuple2<Object, Object>>() {
            private static final long serialVersionUID = 1L;

            public Tuple2<Object, Object> call(LabeledPoint p) {
              Double score = classifier.predict(p.features());
              return new Tuple2<Object, Object>(score, p.label());
            }
          }
        );

        // Get evaluation metrics.
        MulticlassMetrics metrics =
          new MulticlassMetrics(JavaRDD.toRDD(scoreAndLabels));
        
        LOG.info(metrics.confusionMatrix().toString());
        System.exit(1);
        return null;
    }

    @Override
    public String[] optionsDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String analyzerDescription() {
        return "A Machine Learning Analyzer which weighs the power of Apache Spark to perform data analysis";
    }

    @Override
    public String getName() {
        return "Spark Analyzer";
    }

}

package edu.drexel.psal.jstylo.machineLearning.spark.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocumentData;
import edu.drexel.psal.jstylo.generics.FeatureData;
import edu.drexel.psal.jstylo.machineLearning.spark.SparkAnalyzer;

public class SparkAnalyzerTest {

	private SparkAnalyzer testSparkAnalyzer;
	
	@Before
	public void setup(){
		testSparkAnalyzer = new SparkAnalyzer();
		
		List<String> featuresName = new ArrayList<>();
		featuresName.add("feature1");
		featuresName.add("feature2");
		featuresName.add("feature3");

		DataMap testDataMap = new DataMap("testDataMap", featuresName);

		testDataMap.initAuthor("Author1");
		testDataMap.initAuthor("Author2");

		FeatureData testFeatureData1 = new FeatureData("feature1", "norm1", 5);
		testFeatureData1.setValue(0.50);
		FeatureData testFeatureData2 = new FeatureData("feature2", "norm2", 10);
		testFeatureData2.setValue(0.40);
		FeatureData testFeatureData3 = new FeatureData("feature3", "norm3", 15);
		testFeatureData3.setValue(0.10);

		ConcurrentHashMap<Integer, FeatureData> testDataValue1 = new ConcurrentHashMap<>();
		testDataValue1.put(1, testFeatureData1);
		testDataValue1.put(2, testFeatureData2);
		testDataValue1.put(3, testFeatureData3);

		Map<String, Integer> normalizationValue = new HashMap<>();
		normalizationValue.put("norm1", 5);
		normalizationValue.put("norm2", 10);
		normalizationValue.put("norm3", 15);

		DocumentData author1Doc1Data = new DocumentData(normalizationValue, testDataValue1);

		testDataMap.addDocumentData("Author1", "author1Doc1", author1Doc1Data);
		testDataMap.addDocumentData("Author1", "author1Doc1", author1Doc1Data);

		testDataMap.addDocumentData("Author2", "author1Doc1", author1Doc1Data);
		testDataMap.addDocumentData("Author2", "author1Doc1", author1Doc1Data);
	
		testSparkAnalyzer.initSuspects(testDataMap);
	}
	
	@Test
	public void optionsDescription_Test_Success(){
		String expectedResult = "No options at this time";
		
		expectedResult.equals(testSparkAnalyzer.optionsDescription());
	}

	@Test
	public void analyzerDescription_Test_Success(){
		String expectedResult = "A Machine Learning Analyzer which weighs the power of Apache Spark to perform data analysis";
		
		expectedResult.equals(testSparkAnalyzer.analyzerDescription());
	}
	
	@Test
	public void getName_Test_Success(){
		String expectedResult = "Spark Analyzer";
		
		expectedResult.equals(testSparkAnalyzer.getName());
	}
	
	@Test
	public void getExperimentMetrics_Test_Success(){
		String expectedResult = "No metrics calculated";
		
		expectedResult.equals(testSparkAnalyzer.getExperimentMetrics());		
	}
	
}

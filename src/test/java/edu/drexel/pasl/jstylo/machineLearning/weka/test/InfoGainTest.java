package edu.drexel.pasl.jstylo.machineLearning.weka.test;

import static org.junit.Assert.*;

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
import edu.drexel.psal.jstylo.machineLearning.weka.InfoGain;


public class InfoGainTest {
	
	private DataMap testDataMap;
		@Before
		public void setup(){
			List<String> featuresName = new ArrayList<>();
			featuresName.add("feature1");
			featuresName.add("feature2");
			featuresName.add("feature3");
			
			testDataMap = new DataMap("testDataMap",featuresName);
			
			testDataMap.initAuthor("Author1");
			testDataMap.initAuthor("Author2");			
			
			FeatureData testFeatureData1 = new FeatureData("feature1", "norm1", 5);
			testFeatureData1.setValue(0.50);
			FeatureData testFeatureData2 = new FeatureData("feature2", "norm2", 10);
			testFeatureData2.setValue(0.40);
			FeatureData testFeatureData3 = new FeatureData("feature3", "norm3", 15);
			testFeatureData3.setValue(0.10);
			
			ConcurrentHashMap<Integer,FeatureData> testDataValue1 = new ConcurrentHashMap<>();
			testDataValue1.put(1, testFeatureData1);
			testDataValue1.put(2, testFeatureData2);
			testDataValue1.put(3, testFeatureData3);
			
			Map<String,Integer> normalizationValue = new HashMap<>();
			normalizationValue.put("norm1",5);
			normalizationValue.put("norm2", 10);
			normalizationValue.put("norm3", 15);
			
			DocumentData author1Doc1Data = new DocumentData(normalizationValue, testDataValue1);
			
			testDataMap.addDocumentData("Author1", "author1Doc1", author1Doc1Data);
			testDataMap.addDocumentData("Author1", "author1Doc1", author1Doc1Data);
			
			testDataMap.addDocumentData("Author2", "author1Doc1", author1Doc1Data);
			testDataMap.addDocumentData("Author2", "author1Doc1", author1Doc1Data);
			
			
		}
		
		@Test
		public void calcInfoGain_Success() throws Exception{
			
			double[][] expectedList = {
					{0.0,0.0},
					{0.0,1.0},
					{0.0,2.0}
			};
			
			// Verify
			assertEquals(expectedList,InfoGain.calcInfoGain(testDataMap));		
		}

		@Test
		public void applyInfoGain_Success() throws Exception{
			double[][] sortedFeature = {
					{0.0,0.0},
					{0.0,1.0},
					{0.0,2.0}
			};
			
			assertEquals(sortedFeature,InfoGain.applyInfoGain(sortedFeature, testDataMap, 3));	
		}
		
		@Test
		public void applyInfoGain2_Success() throws Exception{
			double[][] sortedFeature = {
					{0.0,0.0}
			};
			
			assertEquals(sortedFeature,InfoGain.applyInfoGain(sortedFeature, testDataMap, 4));	
		}
		
		@Test
		public void applyInfoGain3_Success() throws Exception{
			double[][] sortedFeature = {
					{2.0,1.0},
					{1.0,0.0},
					{3.0,2.0}
			};
			double[][] expectedFeature = {
					{2.0,1.0},
					{1.0,0.0}
			};
			
			assertEquals(expectedFeature,InfoGain.applyInfoGain(sortedFeature, testDataMap, 2));	
		}
}

package edu.drexel.psal.jstylo.generics.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.drexel.psal.jstylo.generics.FeatureData;
import edu.drexel.psal.jstylo.generics.DocumentData;

public class DocumentDataTest {

	private DocumentData testDocumentData; 
	private Map<String, Integer> testNormalizationValues;
	private ConcurrentHashMap<Integer,FeatureData> testDataValues;


	@Before
	public void setup(){
		testNormalizationValues = new HashMap<>();
		testNormalizationValues.put("a",1);
		testNormalizationValues.put("b",2);
		testNormalizationValues.put("c",3);
		testDataValues = new ConcurrentHashMap<>();
		testDataValues.put(1,Mockito.mock(FeatureData.class));
		testDataValues.put(2,Mockito.mock(FeatureData.class));
		testDataValues.put(3,Mockito.mock(FeatureData.class));
		
		testDocumentData = new DocumentData(testNormalizationValues, testDataValues);
	}
	
	@Test
	public void getNormalizationValues_Test(){
		//Verification
		assertEquals(testNormalizationValues,testDocumentData.getNormalizationValues());
	}
	
	
	@Test
	public void getDataValues_Test(){
		//Verification
		assertEquals(testDataValues,testDocumentData.getDataValues());
	}

	@Test
	public void getFeatureCountAtIndex_Test(){
		// Setup
		FeatureData mockFeatureData = Mockito.mock(FeatureData.class);
		testDataValues.put(4,mockFeatureData);
		
		Integer notExistingKey = -1;
		
		// Verification
		assertTrue(-1 == testDocumentData.getFeatureCountAtIndex(notExistingKey));
		assertEquals(mockFeatureData.getCount(), testDocumentData.getFeatureCountAtIndex(4));
	}
	
	@Test
	public void replaceFeatureValues_Test(){
		// Setup
		ConcurrentHashMap<Integer, FeatureData> newDataValue = new ConcurrentHashMap<>();
		newDataValue.put(1,Mockito.mock(FeatureData.class));
		
		// Execution
		testDocumentData.replaceFeatureValues(newDataValue);
		
		// Verification
		assertEquals(newDataValue, testDocumentData.getDataValues());
		
		
	}
}

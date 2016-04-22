package edu.drexel.pasl.jstylo.generics.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.drexel.psal.jstylo.generics.FeatureData;

public class FeatureDataTest {
	private FeatureData testFeatureData;
	
	@Before
	public void setup(){
		testFeatureData = new FeatureData("testFeatureData", "normalization", 2);
	}
	
	@Test
	public void setValue_Success() {
		// Setup
		Double expectedValue = 0.7;
		
		// Execution
		testFeatureData.setValue(expectedValue);
		// Verify
		assertEquals(expectedValue,testFeatureData.getValue());
	}
	
	@Test
	public void getName_Success(){
		// Verify
		assertEquals("testFeatureData", testFeatureData.getName());
	}

	@Test
	public void getNormalizationType_Success(){
		// Verify
		assertEquals("normalization", testFeatureData.getNormalizationType());
	}
	
	@Test
	public void getCount_Success(){
		// Verify
		assertTrue(2==testFeatureData.getCount());
	}
}

package edu.drexel.psal.jstylo.generics.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import edu.drexel.psal.jstylo.generics.DocResult;

public class DocResultTest {

	private DocResult testDocResult;
	
	@Before
	public void setup(){
		Map<String,Double> testProbability = new HashMap<String,Double>();
		testProbability.put("A",0.7);
		testProbability.put("B",0.2);
		testProbability.put("C",0.1);
		
		String title = "testTitle";
		
		String actualAuthor = "testActualAuthor";
		
		testDocResult = new DocResult(title);	
		testDocResult = new DocResult(title,actualAuthor);
		testDocResult = new DocResult(title,testProbability);
		testDocResult = new DocResult(title, actualAuthor, testProbability);	
	}
	
	
	@Test
	public void designateSuspect_InputNewSuspectAndSuspectSet_Success(){
		// Setup
		String testSuspect = "testSuspect";
		List<String> suspectSet = new ArrayList<>();
		suspectSet.add("SuspectA");
		suspectSet.add("SuspectB");
		suspectSet.add("SuspectC");
	
		// Execution
		testDocResult.designateSuspect(testSuspect, suspectSet);
		
		// Verify
		assertEquals(testSuspect,testDocResult.getSuspectedAuthor());
	}
	
	@Test
	public void designateSuspect_ProbabilityMapIsNull_Success(){
		// Setup
		testDocResult = new DocResult("testTitle","testActualAuthor");
		String testSuspect = "testSuspect";
		List<String> suspectSet = new ArrayList<>();
		suspectSet.add("SuspectA");
		suspectSet.add("SuspectB");
		suspectSet.add("testSuspect");
	
		// Execution
		testDocResult.designateSuspect(testSuspect, suspectSet);
		
		// Verify
		assertEquals(testSuspect,testDocResult.getSuspectedAuthor());
	}
	
	@Test
	public void getTitle_Success(){
		// Verify
		assertEquals("testTitle",testDocResult.getTitle());
	}

	@Test
	public void getActualAuthor_Success(){
		// Verify
		assertEquals("testActualAuthor",testDocResult.getActualAuthor());
	}
	
	@Test
	public void getProbabilities_ReturnProbabilities_Success(){
		// Setup
		Map<String,Double> testProbability = new HashMap<String,Double>();
		testProbability.put("A",0.7);
		testProbability.put("B",0.2);
		testProbability.put("C",0.1);
		
		// Verify
		assertEquals(testProbability,testDocResult.getProbabilities());
	}
	
	@Test
	public void getProbabilities_ProbabilityMapIsNull_Success(){
		// Setup
		Map<String,Double> expectedProbability = new HashMap<String,Double>();
		expectedProbability.put("SuspectA",0.0);
		expectedProbability.put("SuspectB",0.0);
		expectedProbability.put("testSuspect",1.0);
		List<String> suspectSet = new ArrayList<>();
		suspectSet.add("SuspectA");
		suspectSet.add("SuspectB");
		suspectSet.add("testSuspect");
		
		//Execution
		testDocResult.designateSuspect("testSuspect", suspectSet);
		//Verify
		assertEquals(expectedProbability,testDocResult.getProbabilities());	
	}
	
	
	@Test
	public void toString_Success(){
		String expectedString = "For document testTitle, the true author is testActualAuthor. After analysis, our top suspect is A with a 0.70 likelihood\n";
		assertEquals(expectedString,testDocResult.toString());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}

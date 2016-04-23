package edu.drexel.pasl.jstylo.generics.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


import edu.drexel.psal.jstylo.generics.DocResult;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import scala.inline;

public class ExperimentResultsTest {
	
	private ExperimentResults testExperimentResults;
	
	@Before
	public void setup(){
		testExperimentResults = new ExperimentResults();
	}
	
	@Test
	public void addDocResult_addNewDocResultObject_Success(){
		// Setup
		DocResult mockDocResult = Mockito.mock(DocResult.class);
		List<DocResult> expectedExperimentContents = new ArrayList<>();
		expectedExperimentContents.add(mockDocResult);
		
		// Execution
		testExperimentResults.addDocResult(mockDocResult);
		
		// Verify
		assertEquals(expectedExperimentContents,testExperimentResults.getExperimentContents());
		
	}
	
	@Test
	public void getTruePositiveRate_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());
		
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		Double expectedValue = 0.5;
			
		// Verify
		assertTrue(expectedValue == testExperimentResults.getTruePositiveRate());
	}
	
	@Test
	public void getCorrectDocCount_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());				
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		int expectedCount = 1;
		
		//Verify
		assertTrue(expectedCount==testExperimentResults.getCorrectDocCount());
	}

	@Test
	public void getStatisticsString_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());				
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		String expectedString = "Correctly Identified 1 out of 2 documents\n\n"
				+ "((Calculated Statistics))\n"
				+ "True Positive Percentage: 50.00\n";
		
		// Verify
		assertEquals(expectedString,testExperimentResults.getStatisticsString());
	}
	
	@Test
	public void getSimpleResults_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());				
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		String expectedString = "((Simple Document Results))\n"
				+ "Document Title | Top Suspect    |\n"
				+ "Title1         | Author1        |\n"
				+ "Title2         | a2             |\n";
		
		// Verify
		assertEquals(expectedString, testExperimentResults.getSimpleResults());
	}
	
	@Test
	public void getAllDocumentResults_True_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());				
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);		
		
		String expectedString = "((Individual Document Results))\n"
				+ "Document Title | Actual         | Top Suspect    | Probability    |\n"
				+ "Title1         | Author1        | Author1        | nu             | Correct!\n"
				+ "Title2         | Author2        | a2             | nu             | Incorrect...\n";
		
		// Verify
		assertEquals(expectedString,testExperimentResults.getAllDocumentResults(true));
	}
	
	@Test
	public void getAllDocumentResults_False_Success(){
		// Setup 
		DocResult testDocResult1 = new DocResult("Title1","Author1");
		testDocResult1.designateSuspect("Author1", new ArrayList<String>());
		DocResult testDocResult2 = new DocResult("Title2", "Author2");
		testDocResult2.designateSuspect("a2", new ArrayList<String>());				
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);		
		String expectedString = "((Individual Document Results))\n"
				+ "Document Title | Actual         | Top Suspect    | Probability    |\n"
				+ "Title1         | Author1        | Author1        | nu             |\n"
				+ "Title2         | Author2        | a2             | nu             |\n";
		
		
		// Verify
		assertEquals(expectedString, testExperimentResults.getAllDocumentResults(false));
	}
	
	@Test
	public void getAllDocumentResultsVerbose_Success(){
		// Setup 
		Map<String, Double> testProbability1 = new HashMap<String, Double>();
		testProbability1.put("Author1",0.7);
		testProbability1.put("Author2",0.2);
		testProbability1.put("Author3",0.1);
		
		Map<String, Double> testProbability2 = new HashMap<String, Double>();
		testProbability2.put("Author1",0.6);
		testProbability2.put("Author2",0.2);
		testProbability2.put("Author3",0.2);
		
		DocResult testDocResult1 = new DocResult("Title1","Author1",testProbability1);
		DocResult testDocResult2 = new DocResult("Title2", "Author2",testProbability2);			
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		String expectedString = "((Verbose Document Results))\n"
				+ "Document Title | Author3    | Author2    | Author1    |\n"
				+ "Title1         |    0.10    |    0.20    |  >>0.70<<  |\n"
				+ "Title2         |    0.20    |    0.20    |  >>0.60<<  |\n";
		
		// Verify
		assertEquals(expectedString, testExperimentResults.getAllDocumentResultsVerbose());
		
	}
	
	@Test
	public void getConfusionMatrix_Success(){
		// Setup 
		Map<String, Double> testProbability1 = new HashMap<String, Double>();
		testProbability1.put("Author1",0.7);
		testProbability1.put("Author2",0.2);
		testProbability1.put("Author3",0.1);
				
		Map<String, Double> testProbability2 = new HashMap<String, Double>();
		testProbability2.put("Author1",0.6);
		testProbability2.put("Author2",0.2);
		testProbability2.put("Author3",0.2);
				
		DocResult testDocResult1 = new DocResult("Title1","Author1",testProbability1);
		DocResult testDocResult2 = new DocResult("Title2", "Author2",testProbability2);			
		testExperimentResults.addDocResult(testDocResult1);
		testExperimentResults.addDocResult(testDocResult2);
		
		String expectedString = "Suspected Authors                                 ||||| Actual Authors|\n"
				+ " Author3        | Author2        | Author1        |||||\n"
				+ "______________________________________________________________________\n"
				+ " 1              | 0              | 0              |||||        Author1|\n"
				+ " 1              | 0              | 0              |||||        Author2|\n"
				+ " 0              | 0              | 0              |||||        Author3|\n";
		
		
		// Verify	
		assertEquals(expectedString, testExperimentResults.getConfusionMatrixString());
	}
	
	
	
	
	
	
}

package edu.drexel.pasl.jstylo.generics.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.drexel.psal.jstylo.featureProcessing.FeatureData;
import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocumentData;

public class DataMapTest {

	private DataMap testDataMap;

	@Before
	public void setUp() {

		List<String> featuresName = new ArrayList<>();
		featuresName.add("Feature0");
		featuresName.add("Feature1");
		featuresName.add("Feature2");

		String DataMapName = "Test_DataMap_Object";

		testDataMap = new DataMap(DataMapName, featuresName);
	}

	@Test
	public void initAuthor_AddNewAuthor_Success() {
		// Setup
		String newAuthorName = "Author Test New";
		// Execution
		testDataMap.initAuthor(newAuthorName);
		// Verification
		assertTrue(testDataMap.getDataMap().containsKey(newAuthorName));

	}

	@Test
	public void addDocumentData_AddNewDocumentData_Success() {
		// Setup
		String newAuthorName = "Author Test New";
		String newDocumentTitle = "NewDoc";
		DocumentData mockDocumentData = Mockito.mock(DocumentData.class);

		// Execution
		testDataMap.initAuthor(newAuthorName);
		testDataMap.addDocumentData(newAuthorName, newDocumentTitle, mockDocumentData);

		// Verification
		assertTrue(newDocumentTitle, testDataMap.getDataMap().containsKey(newAuthorName));
		assertEquals(mockDocumentData, testDataMap.getDataMap().get(newAuthorName).get(newDocumentTitle));
	}

	@Test
	public void getFeatures_ReturnFeatures_Success() {
		// Setup
		Map<Integer, String> testFeatures = new HashMap<>();
		testFeatures.put(0, "Feature0");
		testFeatures.put(1, "Feature1");
		testFeatures.put(2, "Feature2");

		// Verification
		assertEquals(testFeatures, testDataMap.getFeatures());
		
	}

	 @Test 
	 public void removeFeatures_RemoveFeaturesByIndexSet_Success(){
	 // Setup
	 Integer[] indicesToRemove = {0,2};
	 Map<Integer,String> expectedNewFeatures = new HashMap<>();
	 expectedNewFeatures.put(0,"Feature1");
	
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		ConcurrentHashMap<Integer,FeatureData> testDataValues = new ConcurrentHashMap<>();
		FeatureData testFeatureData = new FeatureData("Feature1","type1",3);
		
		testDataValues.put(0,testFeatureData);
		testDataValues.put(1,testFeatureData);
		testDataValues.put(2,testFeatureData);
		
		Map<String,Integer> testNorms = new HashMap<>();
		testNorms.put("String0",0);
		testNorms.put("String1",1);
		testNorms.put("String2",2);
		
		
		DocumentData testDocData = new DocumentData(testNorms, testDataValues);
		

		System.out.println(testDocData.getDataValues().toString());
		
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_3", testDocData);
	
	 // Execution
	 testDataMap.removeFeatures(indicesToRemove);
	
	 // Verify
	 assertEquals(expectedNewFeatures,testDataMap.getFeatures());

	 }

	 @Test 
	 public void removeFeatures2_RemoveFeaturesByIndexSet_Success(){
	 // Setup
	 Integer[] indicesToRemove = {1};
	 Map<Integer,String> expectedNewFeatures = new HashMap<>();
	 expectedNewFeatures.put(0,"Feature0");
	 expectedNewFeatures.put(1,"Feature2");
	
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		ConcurrentHashMap<Integer,FeatureData> testDataValues = new ConcurrentHashMap<>();
		FeatureData testFeatureData = new FeatureData("Feature1","type1",3);
		
		testDataValues.put(0,testFeatureData);
		testDataValues.put(1,testFeatureData);
		testDataValues.put(2,testFeatureData);
		
		Map<String,Integer> testNorms = new HashMap<>();
		testNorms.put("String0",0);
		testNorms.put("String1",1);
		testNorms.put("String2",2);
		
		
		DocumentData testDocData = new DocumentData(testNorms, testDataValues);
		

		System.out.println(testDocData.getDataValues().toString());
		
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_3", testDocData);
	
	 // Execution
	 testDataMap.removeFeatures(indicesToRemove);
	
	 // Verify
	 assertEquals(expectedNewFeatures,testDataMap.getFeatures());
	 }

	 
	 @Test 
	 public void removeFeatures3_RemoveFeaturesByIndexSet_Success(){
	 // Setup
	 Integer[] indicesToRemove = {0};
	 Map<Integer,String> expectedNewFeatures = new HashMap<>();
	 expectedNewFeatures.put(0,"Feature1");
	 expectedNewFeatures.put(1,"Feature2");
	
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		ConcurrentHashMap<Integer,FeatureData> testDataValues = new ConcurrentHashMap<>();
		FeatureData testFeatureData = new FeatureData("Feature1","type1",3);
		
		testDataValues.put(1,testFeatureData);
		testDataValues.put(2,testFeatureData);
		testDataValues.put(3,testFeatureData);
		
		Map<String,Integer> testNorms = new HashMap<>();
		testNorms.put("String0",1);
		testNorms.put("String1",2);
		testNorms.put("String2",3);
		
		
		DocumentData testDocData = new DocumentData(testNorms, testDataValues);
		

		System.out.println(testDocData.getDataValues().toString());
		
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_3", testDocData);
	
	 // Execution
	 testDataMap.removeFeatures(indicesToRemove);
	
	 // Verify
	 assertEquals(expectedNewFeatures,testDataMap.getFeatures());
	 }
	 
	 @Test 
	 public void removeFeatures4_RemoveFeaturesByIndexSet_Success(){
	 // Setup
	 Integer[] indicesToRemove = {2,0};
	 Map<Integer,String> expectedNewFeatures = new HashMap<>();
	 expectedNewFeatures.put(0,"Feature1");
	
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		ConcurrentHashMap<Integer,FeatureData> testDataValues = new ConcurrentHashMap<>();
		FeatureData testFeatureData = new FeatureData("Feature1","type1",3);
		
		testDataValues.put(0,testFeatureData);
		testDataValues.put(1,testFeatureData);
		testDataValues.put(2,testFeatureData);
		
		Map<String,Integer> testNorms = new HashMap<>();
		testNorms.put("String0",0);
		testNorms.put("String1",1);
		testNorms.put("String2",2);
		
		
		DocumentData testDocData = new DocumentData(testNorms, testDataValues);
		

		System.out.println(testDocData.getDataValues().toString());
		
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", testDocData);
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_3", testDocData);
	
	 // Execution
	 testDataMap.removeFeatures(indicesToRemove);
	
	 // Verify
	 assertEquals(expectedNewFeatures,testDataMap.getFeatures());

	 }
	 
	@Test
	public void numDocuments_ReturnNumOfDocuments_Success() {
		// Setup
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_3", Mockito.mock(DocumentData.class));

		// Verify
		assertTrue(5 == testDataMap.numDocuments());
	}

	@Test
	public void getFeatureIndex_EnterFeatureName_ReturnFeatureIndex() {
		// Verify
		assertTrue(-1 == testDataMap.getFeatureIndex("DoNothingFeature"));
		assertTrue(0 == testDataMap.getFeatureIndex("Feature0"));
	}

	@Test
	public void getDocumentTitles_ReturnListOfDocumentTitle_Success() {
		// Setup
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", Mockito.mock(DocumentData.class));

		List<String> expectedTitle = new ArrayList<>();
		expectedTitle.add("Author_1_Doc_1");
		expectedTitle.add("Author_1_Doc_2");
		expectedTitle.add("Author_2_Doc_1");
		expectedTitle.add("Author_2_Doc_2");

		// Verify
		assertEquals(expectedTitle, testDataMap.getDocumentTitles());
	}

	
	@Test
	public void getTitlesToAuthor_ReturnDocAuthorMap_Success(){
		// Setup
		testDataMap.initAuthor("Author_1");
		testDataMap.initAuthor("Author_2");
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_1", "Author_1_Doc_2", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_1", Mockito.mock(DocumentData.class));
		testDataMap.addDocumentData("Author_2", "Author_2_Doc_2", Mockito.mock(DocumentData.class));
		
		Map<String,String> docTitleAutorMap = new HashMap<>();
		docTitleAutorMap.put("Author_1_Doc_1","Author_1");
		docTitleAutorMap.put("Author_1_Doc_2","Author_1");
		docTitleAutorMap.put("Author_2_Doc_1","Author_2");
		docTitleAutorMap.put("Author_2_Doc_2","Author_2");
		
		// Verify
		assertEquals(docTitleAutorMap,testDataMap.getTitlesToAuthor());
	}

}

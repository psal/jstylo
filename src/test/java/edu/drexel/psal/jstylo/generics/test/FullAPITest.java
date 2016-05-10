package edu.drexel.psal.jstylo.generics.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import edu.drexel.psal.jstylo.featureProcessing.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.featureProcessing.ProblemSet;
import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.FullAPI;
import edu.drexel.psal.jstylo.generics.FullAPI.analysisType;
import edu.drexel.psal.jstylo.generics.Preferences;
import edu.drexel.psal.jstylo.machineLearning.Analyzer;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;

public class FullAPITest {
	
	
	private FullAPI testFullAPI;
	private FullAPI.Builder testBuild;
	
	@Before
	public void setup(){
		testBuild = new FullAPI.Builder();		
		testBuild = testBuild.preferences(Preferences.buildDefaultPreferences());
		testBuild = testBuild.psPath("testPsXML");
		testBuild = testBuild.setApplyInfoGain(true);
		testBuild = testBuild.setFeaturesToKeep(1);
		testBuild = testBuild.cfdPath("Test");
		
		Analyzer mockAnalyzer = Mockito.mock(Analyzer.class);
		testBuild = testBuild.setAnalyzer(mockAnalyzer);
		
		ProblemSet mockProblemSet = Mockito.mock(ProblemSet.class);
		testBuild = testBuild.ps(mockProblemSet);
		
		CumulativeFeatureDriver mockCumulativeFeatureDriver = Mockito.mock(CumulativeFeatureDriver.class);
		testBuild = testBuild.cfd(mockCumulativeFeatureDriver);
		
		testBuild = testBuild.numThreads(1);
		
		testBuild = testBuild.preferences(null);
		testBuild = testBuild.numThreads(3);
		
		testBuild = testBuild.numFolds(1);

		testBuild = testBuild.analysisType(analysisType.TRAIN_TEST_KNOWN);
		
		testBuild = testBuild.useCache(true);
		
		testBuild = testBuild.chunkDocs(true);
		testBuild = testBuild.loadDocContents(true);
		testBuild = testBuild.verifierName("test");
		
		testFullAPI = testBuild.build();
		
		testFullAPI.prepareInstances();
		
		testFullAPI.calcInfoGain();
		
		testFullAPI.setUseCache(true);	
		
		testFullAPI.setProblemSet(Mockito.mock(ProblemSet.class));
		
		testFullAPI.setTrainingDataMap(Mockito.mock(DataMap.class));
		testFullAPI.setTestingDataMap(Mockito.mock(DataMap.class));
		
		testFullAPI.setExperimentType(analysisType.TRAIN_TEST_KNOWN);
		testFullAPI.setNumFolds(1);
		testFullAPI.setNumThreads(1);
	}
	
	@Test
	public void isUsingCache_Success() {
		assertTrue(testFullAPI.isUsingCache());
	}
	

}

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
import edu.drexel.psal.jstylo.machineLearning.spark.SparkAnalyzer;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;

public class FullAPITest {
	
	
	private FullAPI testFullAPI;
	private FullAPI.Builder testBuild;
	
	@Before
	public void setup(){
		testBuild = new FullAPI.Builder();		
		testBuild = testBuild.preferences(Preferences.buildDefaultPreferences());
		testBuild = testBuild.psPath("./jsan_resources/problem_sets/drexel_1_small.xml");
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
		testBuild = testBuild.numThreads(4);
		
		testBuild = testBuild.numFolds(1);

		testBuild = testBuild.analysisType(analysisType.CROSS_VALIDATION);
		
		testBuild = testBuild.useCache(false);
		
		testBuild = testBuild.chunkDocs(false);
		testBuild = testBuild.loadDocContents(true);
		testBuild = testBuild.verifierName("test");
		
		testFullAPI = testBuild.build();
		
		//testFullAPI.prepareInstances();
		
		testFullAPI.calcInfoGain();
		
		testFullAPI.setUseCache(false);	
		
		testFullAPI.setProblemSet(Mockito.mock(ProblemSet.class));
		
		testFullAPI.setTrainingDataMap(Mockito.mock(DataMap.class));
		testFullAPI.setTestingDataMap(Mockito.mock(DataMap.class));
		
		testFullAPI.setExperimentType(analysisType.TRAIN_TEST_KNOWN);
		testFullAPI.setNumFolds(1);
		testFullAPI.setNumThreads(1);
	}
	
	@Test
	public void isUsingCache_Success() {
		assertTrue(!testFullAPI.isUsingCache());
	}
	
	@Test
	public void testCrossValidationWEKA(){
        FullAPI test = new FullAPI.Builder()
                .cfdPath("./jsan_resources/feature_sets/9_features.xml")
                .psPath("./jsan_resources/problem_sets/drexel_1_small.xml")
                .setAnalyzer(new WekaAnalyzer())
                .numThreads(4).analysisType(analysisType.CROSS_VALIDATION)
                .useCache(false).chunkDocs(false)
                .loadDocContents(false)
                .numFolds(3)
                .build();
        test.prepareInstances();
        test.calcInfoGain();
        test.run();
        test.getClassificationAccuracy();
        test.getStatString();
        test.getReadableInfoGain(false);
        test.getReadableInfoGain(true);
        Analyzer a = test.getUnderlyingAnalyzer();
        a.getExperimentMetrics();
        a.getLastAuthors();
        a.getLastTrainingSet();
        a.getName();
        a.getOptions();
	}
	
	@Test 
	public void testTrainTestKnownWEKA(){
        FullAPI test = new FullAPI.Builder()
                .cfdPath("./jsan_resources/feature_sets/9_features.xml")
                .psPath("./jsan_resources/problem_sets/drexel_1_train_test.xml")
                .setAnalyzer(new WekaAnalyzer())
                .numThreads(4).analysisType(analysisType.TRAIN_TEST_KNOWN)
                .useCache(false).chunkDocs(false)
                .loadDocContents(false)
                .numFolds(3)
                .build();
        test.prepareInstances();
        test.run();
        test.getStatString();
        Analyzer a = test.getUnderlyingAnalyzer();
        a.getExperimentMetrics();
        a.getLastAuthors();
        a.getLastTrainingSet();
        a.getLastTestSet();
        a.getName();
        a.getOptions();
	}
	
	   @Test 
	    public void testTrainTestUnKnownWEKA(){
	        FullAPI test = new FullAPI.Builder()
	                .cfdPath("./jsan_resources/feature_sets/9_features.xml")
	                .psPath("./jsan_resources/problem_sets/drexel_1_train_test.xml")
	                .setAnalyzer(new WekaAnalyzer())
	                .numThreads(4).analysisType(analysisType.TRAIN_TEST_UNKNOWN)
	                .useCache(false).chunkDocs(false)
	                .loadDocContents(false)
	                .numFolds(3)
	                .build();
	        test.prepareInstances();
	        test.run();
	        test.getStatString();
	        Analyzer a = test.getUnderlyingAnalyzer();
	        a.getExperimentMetrics();
	        a.getLastAuthors();
	        a.getLastTrainingSet();
	        a.getLastTestSet();
	        a.getName();
	        String[] ops = {"a","b","c"};
	        a.setOptions(ops);
	        a.getOptions();
	        ((WekaAnalyzer) a).analyzerDescription();
	        ((WekaAnalyzer) a).getOptions();
	        ((WekaAnalyzer) a).optionsDescription();
	        ((WekaAnalyzer) a).runCrossValidation(test.getTrainingDataMap(), 10, 1L, 2);
	    }
	   
	    @Test
	    public void testCrossValidationSpark(){
	        FullAPI test = new FullAPI.Builder()
	                .cfdPath("./jsan_resources/feature_sets/9_features.xml")
	                .psPath("./jsan_resources/problem_sets/drexel_1_small.xml")
	                .setAnalyzer(new SparkAnalyzer())
	                .numThreads(4).analysisType(analysisType.CROSS_VALIDATION)
	                .useCache(false).chunkDocs(false)
	                .loadDocContents(false)
	                .numFolds(3)
	                .build();
	        test.prepareInstances();
	        test.calcInfoGain();
	        test.run();
	        test.getClassificationAccuracy();
	        test.getStatString();
	        test.getReadableInfoGain(false);
	        test.getReadableInfoGain(true);
	        Analyzer a = test.getUnderlyingAnalyzer();
	        a.getExperimentMetrics();
	        a.getLastAuthors();
	        a.getLastTrainingSet();
	        a.getName();
	        a.getOptions();
	    }
	    
	    @Test 
	    public void testTrainTestKnownSpark(){
	        FullAPI test = new FullAPI.Builder()
	                .cfdPath("./jsan_resources/feature_sets/9_features.xml")
	                .psPath("./jsan_resources/problem_sets/drexel_1_train_test.xml")
	                .setAnalyzer(new SparkAnalyzer("local[*]"))
	                .numThreads(4).analysisType(analysisType.TRAIN_TEST_KNOWN)
	                .useCache(false).chunkDocs(false)
	                .loadDocContents(false)
	                .numFolds(3)
	                .build();
	        test.prepareInstances();
	        test.run();
	        test.getStatString();
	        Analyzer a = test.getUnderlyingAnalyzer();
	        a.getExperimentMetrics();
	        a.getLastAuthors();
	        a.getLastTrainingSet();
	        a.getLastTestSet();
	        a.getName();
	        a.getOptions();
	    }
	    
	       @Test 
	        public void testTrainTestUnKnownSpark(){
	            FullAPI test = new FullAPI.Builder()
	                    .cfdPath("./jsan_resources/feature_sets/9_features.xml")
	                    .psPath("./jsan_resources/problem_sets/drexel_1_train_test.xml")
	                    .setAnalyzer(new SparkAnalyzer())
	                    .numThreads(4).analysisType(analysisType.TRAIN_TEST_UNKNOWN)
	                    .useCache(false).chunkDocs(false)
	                    .loadDocContents(false)
	                    .numFolds(3)
	                    .build();
	            test.prepareInstances();
	            test.run();
	            test.getStatString();
	            Analyzer a = test.getUnderlyingAnalyzer();
	            a.getExperimentMetrics();
	            a.getLastAuthors();
	            a.getLastTrainingSet();
	            a.getLastTestSet();
	            a.getName();
	            String[] ops = {"a","b","c"};
	            a.setOptions(ops);
	            a.getOptions();
	        }
}

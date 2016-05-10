package edu.drexel.psal.jstylo.machineLearning.weka.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.netlib.util.booleanW;

import com.jgaap.generics.Document;
import com.typesafe.config.ConfigException.Null;

import edu.drexel.psal.jstylo.generics.DataMap;
import edu.drexel.psal.jstylo.generics.DocResult;
import edu.drexel.psal.jstylo.generics.DocumentData;
import edu.drexel.psal.jstylo.generics.ExperimentResults;
import edu.drexel.psal.jstylo.generics.FeatureData;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.experiment.Experiment;

public class WekaAnalyzerTest {

	private WekaAnalyzer testWekaAnalyzer;

	@Before
	public void setup() {

		Classifier mockClassifier = Mockito.mock(Classifier.class);
		testWekaAnalyzer = new WekaAnalyzer(mockClassifier);

		Object mockObject = Mockito.mock(Classifier.class);
		testWekaAnalyzer = new WekaAnalyzer(mockObject);

		testWekaAnalyzer = new WekaAnalyzer();
	}

	@Test
	public void classifyWithUnknownAuthors_Test() {
		// setup

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

		List<Document> testUnknowDocs = new ArrayList<>();
		Document testDocmument1 = new Document("fakePath1", "author1", "author1_doc1");
		Document testDocmument2 = new Document("fakePath2", "author2", "author2_doc1");
		testUnknowDocs.add(testDocmument1);
		testUnknowDocs.add(testDocmument2);

		ExperimentResults ExpectedExperimentResults = testWekaAnalyzer.classifyWithUnknownAuthors(testDataMap,
				testDataMap, testUnknowDocs);

		ExperimentResults expectedExperimentResults = new ExperimentResults();

		Map<String, Double> testProbability = new HashMap<>();
		testProbability.put("Author2", 1.0);
		testProbability.put("Author1", 0.0);

		DocResult testDocResult1 = new DocResult("author1Doc1", "_Unknown_", testProbability);
		// testDocResult1.designateSuspect("Author2", null);
		DocResult testDocResult2 = new DocResult("author1Doc1", "_Unknown_", testProbability);
		// testDocResult2.designateSuspect("Author2", null);
		expectedExperimentResults.addDocResult(testDocResult1);
		expectedExperimentResults.addDocResult(testDocResult2);

		// Verify
		expectedExperimentResults.equals(ExpectedExperimentResults);
	}

	@Test
	public void classifyWithKnownAuthors_Test_Success() throws Exception {
		// setup

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

		ExperimentResults expectedExperimentResults = new ExperimentResults();

		Map<String, Double> testProbability = new HashMap<>();
		testProbability.put("Author2", 1.0);
		testProbability.put("Author1", 0.0);

		DocResult testDocResult1 = new DocResult("author1Doc1", "author1", testProbability);
		// testDocResult1.designateSuspect("Author2", null);
		DocResult testDocResult2 = new DocResult("author1Doc1", "author1", testProbability);
		// testDocResult2.designateSuspect("Author2", null);
		expectedExperimentResults.addDocResult(testDocResult1);
		expectedExperimentResults.addDocResult(testDocResult2);

		expectedExperimentResults.equals(testWekaAnalyzer.classifyWithKnownAuthors(testDataMap, testDataMap));

	}

	@Test
	public void getName_Test_Success() {
		// setup

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

		String expectedResult = "WekaAnalyzer with weka.classifiers.functions.SMO classifier";

		expectedResult.equals(testWekaAnalyzer.getName());

	}

	@Test
	public void optionsDescription_Test_Success() {
		// setup

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

		String[] expectedResults = {
				"D<ARG>	If set, classifier is run in debug mode and\n	may output additional info to the console",
				"no-checks<ARG>	Turns off all checks - use with caution!\n"

						+ "	Turning them off assumes that data is purely numeric, doesn't\n"
						+ "	contain any missing values, and has a nominal class. Turning them\n"
						+ "	off also means that no header information will be stored if the\n"
						+ "	machine is linear. Finally, it also assumes that no instance has\n"
						+ "	a weight equal to 0.\n" + "	(default: checks on)"

				, "C<ARG>	The complexity constant C. (default 1)"

				, "N<ARG>	Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)"

				, "L<ARG>	The tolerance parameter. (default 1.0e-3)"

				, "P<ARG>	The epsilon for round-off error. (default 1.0e-12)"

				, "M<ARG>	Fit logistic models to SVM outputs. "

				,
				"V<ARG>	The number of folds for the internal\n" + "	cross-validation. (default -1, use training data)"

				, "W<ARG>	The random number seed. (default 1)"

				, "K<ARG>	The Kernel to use.\n" + "	(default: weka.classifiers.functions.supportVector.PolyKernel)"

				, "<ARG>"

				, "D<ARG>	Enables debugging output (if available) to be printed.\n" + "	(default: off)"

				, "no-checks<ARG>	Turns off all checks - use with caution!\n" + "	(default: checks on)"

				, "C<ARG>	The size of the cache (a prime number), 0 for full cache and \n"
						+ "	-1 to turn it off.\n" + "	(default: 250007)"

				, "E<ARG>	The Exponent to use.\n" + "	(default: 1.0)"

				, "L<ARG>	Use lower-order terms.\n" + "	(default: no)" };

		String[] aa = testWekaAnalyzer.optionsDescription();
		assertEquals(expectedResults, aa);
	}


	@Test
	public void getExperimentMetrics_Test_Success() {
		// setup

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

		System.out.println(testWekaAnalyzer.getExperimentMetrics());
	}
}

package edu.drexel.psal.jstylo.test;

import edu.drexel.psal.JSANConstants;
import edu.drexel.psal.jstylo.featureProcessing.Chunker;
import edu.drexel.psal.jstylo.generics.FullAPI;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import edu.drexel.psal.jstylo.machineLearning.weka.WekaAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.*;

import com.jgaap.generics.Document;

/**
 *
 * @author Andrew DiNunzio and Bekah Overdorf
 */
public class CacheTests
{
	private static final String CHUNKED_DIR_NAME = "chunked";
	
	/**
	 * Do some pre-test stuff, like clean out the temp directory.
	 */
	@BeforeClass
	public static void testSetup(){
		// TODO: nothing really needed here yet.
	}
	
	/**
	 * Stuff to do after we're all done.
	 */
	@AfterClass
	public static void testTearDown() {
		Chunker.shouldChunkTrainDocs(true);
		deleteRecursive(Paths.get(JSANConstants.JSAN_CACHE).toFile(), true);
		deleteRecursive(Paths.get(JSANConstants.JUNIT_RESOURCE_PACKAGE, "temp").toFile(), false);
	}
	
	/**
	 * Before each test, do some preparation.
	 */
	@Before
	public void setUp() {
		Chunker.shouldChunkTrainDocs(true);
		deleteRecursive(Paths.get(JSANConstants.JSAN_CACHE).toFile(), true);
		deleteRecursive(Paths.get(JSANConstants.JUNIT_RESOURCE_PACKAGE, "temp").toFile(), false);
		
	}
	
	/**
	 * After each test, do some clean-up
	 */
	@After
	public void tearDown() {
		// TODO: nothing really needed here yet.
	}
	
	/**
	 * Test to make sure that chunking does not change the results.
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Test
	public void testNoCache() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		ProblemSet ps = new ProblemSet(Paths.get(JSANConstants.JSAN_PROBLEMSETS_PREFIX, "drexel_1_small.xml").toString());
		Document d = ps.trainDocAt("a", "a_01.txt");
		Assert.assertNotNull("No document a_01.txt found.", d);
		ps.removeTrainDocAt("a", d);
		ps.addTestDoc("a", d);
        Path path = Paths.get(JSANConstants.JSAN_FEATURESETS_PREFIX,"writeprints_feature_set_limited.xml");
		FullAPI test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false)
                .useCache(false).build();
		long bef1 = System.currentTimeMillis();
		test.prepareInstances();
        test.run();
        long aft1 = System.currentTimeMillis();
        String results1 = test.getStatString();
        System.out.println(results1);
        
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false)
                .useCache(false).build();
		long bef2 = System.currentTimeMillis();
		test.prepareInstances();
        test.run();
        long aft2 = System.currentTimeMillis();
        String results2 = test.getStatString();
        System.out.println(results2);
        
        long time1 = aft1 - bef1;
        long time2 = aft2 - bef2;
        
        // This assertion may be too lenient. Just trying to make sure that the time it takes the
        // second time is close to the time it takes the first time (in other words, so we know for sure
        // it did not use any extracted features)
        double percentDiff = (double)(Math.abs(time1 - time2))/time2;
        System.out.println("Percent difference between two runs: " + percentDiff);
        
        File cache = Paths.get(JSANConstants.JSAN_CACHE).toFile();
        File[] contents = cache.listFiles();
        Assert.assertNotNull("No files in cache. Chunking directory should be there.", contents);
        boolean foundChunkingDirectory = false;
        for (File f : contents) {
        	if(f.isDirectory() && f.getName().equals(CacheTests.CHUNKED_DIR_NAME))
        		foundChunkingDirectory = true;
        	else {
        		Assert.fail("The caching system made directories/files when caching was turned off.");
        	}
        }
        Assert.assertTrue("Chunking was turned on, but no chunking directory was found.", foundChunkingDirectory);
	}
	
	/**
	 * Test to make sure that chunking does not change the results.
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Test
	public void testWithoutChunking() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		// TODO: this test is basically encapsulated in testWithChunking... so maybe get rid of this?
		Chunker.shouldChunkTrainDocs(false);
		ProblemSet ps = new ProblemSet(Paths.get(JSANConstants.JSAN_PROBLEMSETS_PREFIX, "drexel_1_small.xml").toString());
		Document d = ps.trainDocAt("a", "a_01.txt");
		Assert.assertNotNull("No document a_01.txt found.", d);
		ps.removeTrainDocAt("a", d);
		ps.addTestDoc("a", d);
        Path path = Paths.get(JSANConstants.JSAN_FEATURESETS_PREFIX,"writeprints_feature_set_limited.xml");
		FullAPI test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results1 = test.getStatString();
        System.out.println(results1);
        
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results2 = test.getStatString();
        System.out.println(results2);
        
        Assert.assertEquals("Cached results different from non-cached results", results1, results2);
	}
	
	/**
	 * Test to make sure that chunking does not change the results of the cache.
	 * It's a long test, but it's very important.
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Test
	public void testWithChunking() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		// the max difference allowed between chunked / non-chunked.
		double maxDifferential = 0.05;
		
		ProblemSet ps = new ProblemSet(Paths.get(JSANConstants.JSAN_PROBLEMSETS_PREFIX, "drexel_1_small.xml").toString());
		Document d = ps.trainDocAt("a", "a_01.txt");
		Assert.assertNotNull("No document a_01.txt found.", d);
		ps.removeTrainDocAt("a", d);
		ps.addTestDoc("a", d);
        Path path = Paths.get(JSANConstants.JSAN_FEATURESETS_PREFIX,"writeprints_feature_set_limited.xml");
		FullAPI test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results1 = test.getStatString();
        System.out.println(results1);
        
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results2 = test.getStatString();
        System.out.println(results2);
        
        Assert.assertEquals("Cached results different from non-cached results", results1, results2);
        
        // make it rechunk, then test to make sure the results are the same.
        deleteRecursive(Paths.get(JSANConstants.JSAN_CHUNK_DIR).toFile(), true);
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results3 = test.getStatString();
        System.out.println(results3);
        Assert.assertEquals("Cached results different from non-cached results", results2, results3);
        
        // now, keep the chunks, but delete the cache, and try again.
        File cacheDir = Paths.get(JSANConstants.JSAN_CACHE).toFile();
        for (File f : cacheDir.listFiles()) {
        	if (f.getName() != "chunked") {
        		deleteRecursive(f,true);
        	}
        }
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results4 = test.getStatString();
        System.out.println(results4);
        Assert.assertEquals("Cached results different from non-cached results", results3, results4);
        
        
        // ----------------------------------------------------------------------------
        // Turn off chunking and make sure the results are the same
        // There is good reason to test to make sure the results for this test are
        // (close to) the same, regardless of whether chunking is on or not.
        // ----------------------------------------------------------------------------
        deleteRecursive(Paths.get(JSANConstants.JSAN_CACHE).toFile(), true);
        Chunker.shouldChunkTrainDocs(false);
        ps = new ProblemSet(Paths.get(JSANConstants.JSAN_PROBLEMSETS_PREFIX, "drexel_1_small.xml").toString());
		d = ps.trainDocAt("a", "a_01.txt");
		Assert.assertNotNull("No document a_01.txt found.", d);
		ps.removeTrainDocAt("a", d);
		ps.addTestDoc("a", d);
        path = Paths.get(JSANConstants.JSAN_FEATURESETS_PREFIX,"writeprints_feature_set_limited.xml");
		test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        
        results4 = results4.trim();
        String res4 = results4.substring(results4.lastIndexOf("\n")+1);
        String[] allRes4 = Pattern.compile("|", Pattern.LITERAL).split(res4);
        String results5 = test.getStatString();
        System.out.println(results5);
        String res5 = results5.trim().substring(results5.trim().lastIndexOf("\n")+1);
        String[] allRes5 = Pattern.compile("|", Pattern.LITERAL).split(res5);
        for (int i=1; i<allRes4.length; i++) {
        	double num = Double.parseDouble(allRes4[i].trim().replace(" +", ""));
        	double num2 = Double.parseDouble(allRes5[i].trim().replace(" +", ""));
        	Assert.assertTrue("There was a large difference between results for chunked / non-chunked." +
        	" \nChunked: "+num+"; Non-chunked: "+num2, (Math.abs((num - num2))/num2 <= maxDifferential));
        }
        
        // Changed test to the one above.. since there WILL be differences between chunked/non-chunked,
        // we should just make sure that the difference isn't huge.
        //Assert.assertEquals("Chunked results different from non-chunked results", results4, results5);
        
        // now do it again with the cache
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results6 = test.getStatString();
        System.out.println(results6);
        
        Assert.assertEquals("Cached results different from non-cached results", results5, results6);
        
        // make it rechunk, then test to make sure the results are the same.
        deleteRecursive(Paths.get(JSANConstants.JSAN_CHUNK_DIR).toFile(), true);
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results7 = test.getStatString();
        System.out.println(results7);
        Assert.assertEquals("Cached results different from non-cached results", results6, results7);
        
        // now, keep the chunks, but delete the cache, and try again.
        for (File f : cacheDir.listFiles()) {
        	if (f.getName() != "chunked") {
        		deleteRecursive(f,true);
        	}
        }
        test = new FullAPI.Builder().cfdPath(path.toString()).ps(ps)
                .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                .numThreads(4).analysisType(FullAPI.analysisType.TRAIN_TEST_UNKNOWN).useDocTitles(false).build();
        test.prepareInstances();
        test.run();
        String results8 = test.getStatString();
        System.out.println(results8);
        Assert.assertEquals("Cached results different from non-cached results", results7, results8);
	}
	
	/*
	 * Tests to make sure the caching system works.
	 * Compares the results of using cached/non-cached features.
	 * 
	 * Takes about 1 minute, depending on the authors selected and how many documents they have.
	 */
	@Test
	public void scalingCached() throws Exception
    {
		// TODO: Tests generally should have no randomness. We should perform these tests on a set of static problem sets
		// that the test uses every time.
		
    	int num_authors = 2;
        System.out.println(num_authors);
        File source = Paths.get(JSANConstants.JSAN_CORPORA_PREFIX, "drexel_1").toFile();
        Assert.assertTrue("You don't have the specified corpora available.", source.exists() && source.isDirectory());
        
        // This can be anything. Problem set location
        File dest = Paths.get(JSANConstants.JUNIT_RESOURCE_PACKAGE, "temp", "cache_test_.xml").toFile();

        GenericProblemSetCreator.createProblemSetsWithSize(source, dest, num_authors, 10);
        File[] problem_sets = BekahUtil.listNotHiddenFiles(dest.getParentFile());

        for(File problem_set : problem_sets)
        {
            // Delete the cache in the beginning
            // File to cache
            deleteRecursive(new File(JSANConstants.JSAN_CACHE), false);

            try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            System.out.println(problem_set);
            ProblemSet p = new ProblemSet(problem_set.getAbsolutePath());
            Set<String> authors = p.getAuthors();
            for(String a : authors)
            {
                System.out.print(a + ",");
            }
            System.out.println("");
            Path path = Paths.get(JSANConstants.JSAN_FEATURESETS_PREFIX,"writeprints_feature_set_limited.xml");
            FullAPI test = new FullAPI.Builder().cfdPath(path.toString())
                    .psPath(problem_set.getAbsolutePath())
                    .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                    .numThreads(4).analysisType(FullAPI.analysisType.CROSS_VALIDATION).useDocTitles(false).build();

            long bef1 = System.currentTimeMillis();
            test.prepareInstances();
            test.run();
            long aft1 = System.currentTimeMillis();
            
            String result1 = test.getEvaluation().toSummaryString();
            System.out.println(result1);
            
            FullAPI test2 = new FullAPI.Builder().cfdPath(path.toString())
                    .psPath(problem_set.getAbsolutePath())
                    .setAnalyzer(new WekaAnalyzer(Class.forName("weka.classifiers.functions.SMO").newInstance()))
                    .numThreads(4).analysisType(FullAPI.analysisType.CROSS_VALIDATION).useDocTitles(false).build();

            long bef2 = System.currentTimeMillis();
            test2.prepareInstances();
            test2.run();
            long aft2 = System.currentTimeMillis();
            String result2 = test2.getEvaluation().toSummaryString();
            System.out.println(result2);
            
            Assert.assertEquals("Cached results different from non-cached results", result1, result2);
            
            System.out.print((aft1 - bef1) / 1000 + " s\t");
            System.out.println((aft2 - bef2) / 1000 + " s");
        }
    }
	
	/**
     * Recursively delete contents of a directory (if f is a directory),
     * then delete f.
     * @param f   			the file/directory to delete
     * @param deleteMe		true if it should delete f after deleting its contents (if any).
     * 
     * @return	true if the contents were successfully deleted. If deleteTop is true, returns true
     * 			if f was successfully deleted.
     */
    public static boolean deleteRecursive(File f, boolean deleteMe) {
    	File[] safeDirs = {new File(JSANConstants.JUNIT_RESOURCE_PACKAGE),
    					new File(JSANConstants.JSAN_CACHE)};
		try {
			for (File dir : safeDirs) {
				if (f.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
					return deleteRecursiveUnsafe(f, deleteMe);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        return false;
    }
    

    /**
     * Recursively delete contents of a directory (if f is a directory),
     * then delete f.
     * @param f   			the file/directory to delete
     * @param deleteMe		true if it should delete f after deleting its contents (if any).
     * 
     * @return	true if the contents were successfully deleted. If deleteTop is true, returns true
     * 			if f was successfully deleted.
     */
    private static boolean deleteRecursiveUnsafe(File f, boolean deleteMe) {
    	boolean isWorking = true;
        if (f.exists()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (!deleteRecursiveUnsafe(files[i], true)) {
                    	isWorking = false;
                    }
                } else {
                	if (!files[i].delete()) {
                		isWorking = false;
                	}
                }
            }
        }
        if (deleteMe)
        	return f.delete();
        return isWorking;
    }
	
}

package edu.drexel.psal.jstylo.test;

import edu.drexel.psal.JSANConstants;
import edu.drexel.psal.jstylo.generics.FullAPI;
import edu.drexel.psal.jstylo.generics.ProblemSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.*;

/**
 *
 * @author bekahoverdorf
 */
public class CacheTests
{
	/*
	 * Do some pre-test stuff, like clean out the temp directory.
	 */
	@BeforeClass
	public static void testSetup(){
		File tempDir = Paths.get(JSANConstants.JUNIT_RESOURCE_PACKAGE, "temp").toFile();
		deleteRecursive(tempDir, false);
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
        BekahUtil.deleteChildren(dest.getParentFile());

        GenericProblemSetCreator.createProblemSetsWithSize(source, dest, num_authors, 10);
        File[] problem_sets = BekahUtil.listNotHiddenFiles(dest.getParentFile());

        for(File problem_set : problem_sets)
        {
            // Delete the cache in the beginning
            // File to cache
            BekahUtil.deleteChildren(new File(JSANConstants.JSAN_CACHE));

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
                    .psPath(problem_set.getAbsolutePath()).classifierPath("weka.classifiers.functions.SMO")
                    .numThreads(4).analysisType(FullAPI.analysisType.CROSS_VALIDATION).useDocTitles(false).build();

            long bef1 = System.currentTimeMillis();
            test.prepareInstances();
            test.prepareAnalyzer();
            test.run();
            long aft1 = System.currentTimeMillis();
            
            String result1 = test.getEvaluation().toSummaryString();
            System.out.println(result1);
            
            FullAPI test2 = new FullAPI.Builder().cfdPath(path.toString())
                    .psPath(problem_set.getAbsolutePath()).classifierPath("weka.classifiers.functions.SMO")
                    .numThreads(4).analysisType(FullAPI.analysisType.CROSS_VALIDATION).useDocTitles(false).build();

            long bef2 = System.currentTimeMillis();
            test2.prepareInstances();
            test2.prepareAnalyzer();
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
     * @param deleteTop		true if it should delete f after deleting its contents (if any).
     * 
     * @return	true if the contents were successfully deleted. If deleteTop is true, returns true
     * 			if f was successfully deleted.
     */
    public static boolean deleteRecursive(File f, boolean deleteMe) {
    	File dir = new File(JSANConstants.JUNIT_RESOURCE_PACKAGE);
        try {
			if (f.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
				return deleteRecursiveUnsafe(f, deleteMe);
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

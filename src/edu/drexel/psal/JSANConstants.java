package edu.drexel.psal;

/**
 * Defines a whole slew of public static constants that can be used system-wide.
 */
public class JSANConstants {
	
	public static final String JSAN_RESOURCE_PACKAGE = "/edu/drexel/psal/resources/";
	public static final String JSAN_GRAPHICS_PREFIX = JSAN_RESOURCE_PACKAGE+"graphics/";
	public static final String JSAN_EXTERNAL_RESOURCE_PACKAGE = "jsan_resources/";
	public static final String JSAN_CACHE = JSAN_EXTERNAL_RESOURCE_PACKAGE + "cache/";
	public static final String JSAN_CORPORA_PREFIX = JSAN_EXTERNAL_RESOURCE_PACKAGE+"corpora/";
	public static final String JSAN_PROBLEMSETS_PREFIX = JSAN_EXTERNAL_RESOURCE_PACKAGE+"problem_sets/";
	public static final String JSAN_FEATURESETS_PREFIX = JSAN_EXTERNAL_RESOURCE_PACKAGE+"feature_sets/";
	public static final String JSAN_CHUNK_DIR = JSAN_CACHE + "chunked/";
	public static final String JGAAP_RESOURCE_WORDNET = JSAN_EXTERNAL_RESOURCE_PACKAGE+"wordnet/";
	public static final String JUNIT_RESOURCE_PACKAGE = "junit_resources/";
	public static final boolean USE_CACHE = true;
	public static final boolean CHUNK_TRAIN_DOCS = true;
	
	public static final int REQUIRED_NUM_OF_WORDS = 3500;
	public static final int CHUNK_MIN_SIZE = 475;
	public static final int CHUNK_DEFAULT_SIZE = 500;
	public static final int CHUNK_MAX_SIZE = Integer.MAX_VALUE;
	/**
	 * The max difference between cached chunk size and desired chunk size allowed before re-chunking.
	 */
	public static final double CHUNK_MAX_DIFFERENTIAL = 0.05;
	
	public static final String DUMMY_NAME = "~* you *~"; // NOTE DO NOT CHANGE THIS unless you have a very good reason to do so.

}

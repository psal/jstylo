package edu.drexel.psal.jstylo.generics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jgaap.generics.Document;

import edu.drexel.psal.JSANConstants;

/**
 * Chunks the training documents into chunks (close to) the size of the (first) test document
 * @author Andrew DiNunzio
 */
public final class Chunker {
	
	private static int chunkMinSize = 475;
	private static int chunkDefaultSize = 500;
	private static int chunkMaxSize = Integer.MAX_VALUE;
	private static boolean chunkTrainDocs = true;
	
	/**
	 * The max difference between cached chunk size and desired chunk size allowed before re-chunking.
	 */
	private static double chunkMaxDifferential = 0.05;
	
	/**
	 * Once the training documents are first chunked, the "original" documents
	 * are stored here. The next time the documents are chunked, they will be loaded from the
	 * stored "original" training documents. That way it does not try to chunk a list of chunked
	 * documents. If the training documents are changed (not the contents, but the list of documents),
	 * including any user sample documents or any training documents by any other author, that author's list
	 * must be updated to that new list.
	 * 
	 * How many chunks could this chunker chunk if this chunker could chunk chunks?
	 */
	private static Map<String, List<Document>> originalAuthorMap = new HashMap<String, List<Document>>();
	
	/**
	 * Returns true if the chunker is "on".
	 * @return
	 */
	public static boolean shouldChunkTrainDocs() {
		return chunkTrainDocs;
	}
	/**
	 * Set whether or not the chunker should be "on" or "off".
	 * This will not change any problem sets previously chunked (i.e. they will still point
	 * to the chunked documents).
	 * @param shouldChunk	What to set the chunker to. true = on; false = off.
	 */
	public static void shouldChunkTrainDocs(boolean shouldChunk) {
		// TODO: maybe upon calling this, the original documents should replace the chunked ones
		// for the problem set...
		chunkTrainDocs = shouldChunk;
	}
	
	/**
	 * Set the training documents the chunker should chunk for a given author.
	 * If the training documents are changed (not the contents, but the list of documents), including
	 * any user sample documents or any training documents by any other author, this list must
	 * be updated to that new list. You can set it to null, and the chunker will just set it to the
	 * list of training docs present in the problem set next time it chunks. Be cautious doing that,
	 * since the chunker does replace the list of training docs in the problem set with the chunked
	 * documents.
	 */
	public static void setOriginalAuthorDocs(String author, List<Document> originalDocs) {
		originalAuthorMap.put(author, originalDocs);
	}
	
	/**
	 * If you want to reset the training documents' author map, you can just set it here.
	 * @param map
	 */
	public static void setOriginalAuthorMap(Map<String, List<Document>> map) {
		originalAuthorMap = map;
	}
	
	/**
	 * @param author The author who you'd like to retrieve the original docs for.
	 * @return The original documents for the given author
	 */
	public static List<Document> getOriginalAuthorDocs(String author) {
		return originalAuthorMap.get(author);
	}
	/**
	 * @return the original author map, set before any chunking occurred.
	 */
	public static Map<String, List<Document>> getOriginalAuthorMap() {
		return originalAuthorMap;
	}
	
	/**
	 * Chunk all of the training documents up for the given problem set.
	 * The chunking size is dependent on the size of the (first) test document, but you can
	 * change the min or max chunk size constants in JSANConstants. Additionally, you 
	 * can set what the max differential allowed is between the cached chunk size and the
	 * desired chunk size for the next set of training documents. This is to reduce the number
	 * of times the feature cache for the chunked documents will become invalid.
	 * @param ps
	 */
	public static void chunkAllTrainDocs(ProblemSet ps) {
		if (!chunkTrainDocs) {
			Logger.logln("WARNING: Chunking is turned off. Training documents will be used as-is.");
			return;
			}
		List<Document> testDocuments = ps.getAllTestDocs();

		int chunkSize = chunkDefaultSize;
		if (testDocuments.size() == 0) {
			Logger.logln("INFO: No test documents found. Will use chunkDefaultSize as default chunk size.");
		} else {
			if (testDocuments.size() > 1) {
				Logger.logln("INFO: More than one test document found. Chunking size for training docs"
						+ " will only be checked against the first.");
			}
			String testDocument = readFile(testDocuments.get(0).getFilePath());
			if (testDocument == null || testDocument.length() == 0) {
				Logger.logln("WARNING: Test document was null or empty. Using chunkDefaultSize as default chunk size.");
				chunkSize = chunkDefaultSize;
			} else {
				chunkSize = testDocument.split("(?<! ) ").length; // (?<! ) 
			}

			// Make sure the chunk size is within the specified boundaries.
			if (chunkSize < chunkMinSize) {
				chunkSize = chunkMinSize;
			} else if (chunkSize > chunkMaxSize) {
				chunkSize = chunkMaxSize;
			}
		}

		Map<String, List<Document>> authorMap = ps.getAuthorMap();
		
		// Chunk documents for each author based on the chunk size determined above.
		for (Map.Entry<String, List<Document>> entry : authorMap.entrySet()) {
			/*
			 If there is a stored value for this author, it is likely that the list of Documents observed here are already
			 chunked. We will load the list we stored as the original instead.
			 One reason to set it back to the original documents instead of just trying to reuse the chunked ones is that
			 the chunker uses the "last modified" values of the original documents to make sure that the training documents that
			 were chunked last have not been modified since.
			 The other reason is that if the program chunks the files, when it goes to chunk them again, it will notice that the
			 "last modified" values are different and invalidate the chunk cache, thus deleting every document in there. Then when it
			 goes to read the files again, it will try to read files that aren't there.
			 */
			if (originalAuthorMap.containsKey(entry.getKey())) {
				entry.setValue(originalAuthorMap.get(entry.getKey()));
			} else {
				originalAuthorMap.put(entry.getKey(), entry.getValue());
			}
			File chunkDir = new File(JSANConstants.JSAN_CHUNK_DIR);

			/*
			 * If the author is "~* you *~" (DUMMY_NAME), do not prefix it with _
			 * so something wonky won't happen if there is an author named
			 * "you" that isn't you for some reason.
			 */
			if (!entry.getKey().equals(JSANConstants.DUMMY_NAME))
				chunkDir = new File(chunkDir, "_" + entry.getKey());
			else
				chunkDir = new File(chunkDir, "you");
			
			if (isChunkCacheValid(chunkDir, entry.getValue(),
					chunkSize, chunkMaxDifferential)) {
				ArrayList<Document> documentList = new ArrayList<Document>();
				File[] chunkFiles = chunkDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File arg0) {
						return arg0.getName().matches("\\d{3,}.txt");
					}
				});
				if (chunkFiles != null) {
					for (File chunkFile : chunkFiles) {
						documentList.add(new Document(chunkFile
								.getAbsolutePath().toString(), entry.getKey(), chunkFile.getName()));
					}
				}
				if (documentList.size() > 0) {
					ps.setTrainDocs(entry.getKey(), documentList);
				} else {
					Logger.logln("Warning: Unable to use previously chunked documents. Using sample docs as is.");
				}

			} else {
				// previously chunked documents are not valid anymore. Need to
				// re-chunk.
				Engine.deleteRecursive(chunkDir);
				makeChunkHash(chunkDir, entry.getValue(), chunkSize);

				// this minChunkSize is different than the MIN_CHUNK_SIZE. The
				// constant is absolute,
				// where this is just trying to provide some flexibility when
				// chunking it to a size
				// based on the number of words in the test doc
				int minChunkSize = (int) (chunkSize * (1 - chunkMaxDifferential));
				if (minChunkSize < chunkMinSize) {
					minChunkSize = chunkMinSize;
				}

				List<Document> newTrainDocs = chunkTrainDocs(entry.getKey(), chunkDir, entry.getValue(), chunkSize,
						minChunkSize);
				if (newTrainDocs != null) {
					ps.setTrainDocs(entry.getKey(), newTrainDocs);
				} else {
					Logger.logln("WARNING: Unable to chunk documents. Using sample docs as is.");
				}
			}
		} // end of for loop over author map
	}
	
	/**
	 * Constructor.
	 */
	private Chunker() {
		// do not instantiate
	}
	
	private static Comparator<Document> sortComparator = new Comparator<Document>() {
		@Override
		public int compare(Document o1, Document o2) {
			return o1.getTitle().compareTo(o2.getTitle());
		}
	};
	
	/**
	 * Check to see if the previously chunked files can still be used.
	 * If it has to rechunk the files, it cannot make use of the cache for the documents, so
	 * there is some leeway in the chunk size differential.
	 * @param desiredChunkSize	The desired size of the chunks. This will be compared to the chunk size
	 *			when the cached chunk files were created.
	 * @param maxDifferential	The max differential allowed between the size of chunks when the 
	 * 			chunk files were created and the current desired chunk size. Ex. 0.05 would
	 * 			mean that any cached chunk size within 5% of the desired chunk size would be acceptable.
	 * @return
	 */
	private static boolean isChunkCacheValid(File chunkDir, List<Document> trainDocs, int desiredChunkSize, double maxDifferential) {
		/*
		 * The chunk hash works as follows.
		 * 
		 * The first line is the number of words per chunk that was used when the sample docs were chunked.
		 * This number can be slightly off from the current desired chunk size 
		 * 
		 * The second line onward is just a list of "last modified" values for the sorted (by title) list
		 * of sample documents. Each line is compared to the "last modified" values of the current sample docs.
		 */
		
		if (!chunkDir.exists() || !chunkDir.isDirectory()) {
			return false;
		}
		
		File chunkHash = new File(chunkDir, "hash.txt");
		if (!chunkHash.exists() || !chunkHash.isFile() || !chunkHash.canRead()) {
			return false;
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(chunkHash));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		int cachedChunkSize = 0;
		boolean isValid = true;
		try {
			cachedChunkSize = Integer.parseInt(reader.readLine());
			if (Math.abs(desiredChunkSize - cachedChunkSize)
					/ (double) desiredChunkSize > maxDifferential) {
				reader.close();
				return false;
			}
			// sort the documents by title
			Collections.sort(trainDocs, sortComparator);

			for (Document sampleDoc : trainDocs) {
				String line = reader.readLine();
				if (line == null) {
					reader.close();
					return false;
				}
				long cachedLastModified = Long.parseLong(line);
				if (new File(sampleDoc.getFilePath()).lastModified() != cachedLastModified) {
					reader.close();
					return false;
				}
			}
			// If there were more documents when the sample docs were last chunked,
			// 		the chunked files are invalid.
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty()){
					reader.close();
					return false;
				}
			}
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
			isValid = false;
		} catch (IOException e1) {
			e1.printStackTrace();
			isValid = false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				isValid = false;
			}
		}
		
		return isValid;
	}
	/**
	 * This will create the hash file for the chunked sample documents. This hash file ensures
	 * that an invalid set of chunked files will not be used.
	 * @param author
	 * @param trainDocs
	 * @param desiredChunkSize
	 */
	private static void makeChunkHash(File chunkDir, List<Document> trainDocs, int desiredChunkSize) {
		/*
		 * The chunk hash works as follows.
		 * 
		 * The first line is the desired number of words per chunk.
		 * 
		 * The second line onward is just a list of "last modified" values for the sorted (by title) list
		 * of documents.
		 */
		
		if (!chunkDir.exists()) {
			chunkDir.mkdirs();
			Logger.logln("chunkDir created: " + chunkDir.getAbsolutePath());
		} else {
			if (!chunkDir.isDirectory()) {
				Logger.logln("WARNING: A file exists with the name of the desired chunking directory.");
				return;
			}
		}
			
		File chunkHash = new File(chunkDir, "hash.txt");
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(chunkHash));
		} catch (IOException e1) {
			e1.printStackTrace();
			Logger.logln("WARNING: Failed to make hash for the chunker.");
			return;
		}

		try {
			writer.write(desiredChunkSize + "\n");
			// sort the documents by title
			Collections.sort(trainDocs, sortComparator);
			for (Document sampleDoc : trainDocs) {
				String lastModified = Long.toString(new File(sampleDoc.getFilePath()).lastModified());
				writer.write(lastModified + "\n");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Chunk the sample documents into roughly equal sized chunks (except perhaps the last one)
	 * @param author	the author of the trainDocs. The docs should all be by this one author.
	 * @param trainDocs	the training documents for the given author
	 * @param chunkSize the size the chunks should default to
	 * @param minChunkSize the minimum chunk size to allow. If there are fewer words left at the end, they are ignored.
	 * @return The list of chunked documents, or null if there are not enough words, as specified by
	 * 		   JSANConstants.REQUIRED_NUM_OF_WORDS or if an error occurs.
	 */
	private static List<Document> chunkTrainDocs(String author, File chunkDir, List<Document> trainDocs, int chunkSize, int minChunkSize) {
		Collections.sort(trainDocs, sortComparator);
		
		StringBuilder sb = new StringBuilder();
		for (Document d : trainDocs) {
			sb.append(readFile(d.getFilePath()) + " ");
		}
		String[] words = sb.toString().trim().split("(?<! ) "); // (?<! ) 

		if (words.length < JSANConstants.REQUIRED_NUM_OF_WORDS) {
			Logger.logln("WARNING: Too few words in author " + author + "'s documents. Expected "
					+ JSANConstants.REQUIRED_NUM_OF_WORDS + " words. Got "
					+ words.length + " words.");
			//return null; // TODO: should I return here?
		}

		ArrayList<Document> documentList = new ArrayList<Document>();

		if (!chunkDir.exists()) {
			Logger.logln("WARNING: Chunking dir not found for author " + author
					+ ". Chunking has been disabled for this author.");
			return null; // it will not chunk the documents; it will
							// just use the training docs as they are
		}

		int maxChunks = words.length / chunkSize + 1; // will have 1 less if
														// the last chunk is
														// less than
														// minChunkSize
														// words
		BufferedWriter writer = null;
		for (int i = 0; i < maxChunks; i++) {
			// will chop off anything after the last multiple of chunkSize
			// if that last chunk is less than minChunkSize words
			int nWords = (i < maxChunks - 1) ? chunkSize : (words.length % chunkSize);
			if (nWords < minChunkSize)
				break;
			sb = new StringBuilder();
			for (int j = 0; j < nWords; j++) {
				sb.append(words[chunkSize * i + j]);
				if (j < nWords - 1)
					sb.append(" ");
			}
			File chunkFile = new File(chunkDir, String.format("%03d.txt", i));
			try {
				writer = new BufferedWriter(new FileWriter(
						chunkFile.getAbsolutePath()));
				writer.write(sb.toString().trim());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			documentList.add(new Document(chunkFile.getAbsolutePath()
					.toString(), author, chunkFile
					.getName()));
		}

		return documentList;
	}
	
	private static String readFile(String filePath) {
		try {
			Path path = Paths.get(filePath);
			if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
				return null;
			byte[] bytes = Files.readAllBytes(path);
			return new String(bytes);
		} catch (IOException e) {
			e.printStackTrace();
			Logger.logln("ERROR: Failed to read file at path: " + filePath);
			return null;
		}
	}

}

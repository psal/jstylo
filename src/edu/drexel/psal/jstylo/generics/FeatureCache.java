package edu.drexel.psal.jstylo.generics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jgaap.generics.Document;
import com.jgaap.generics.Event;
import com.jgaap.generics.EventSet;

import edu.drexel.psal.JSANConstants;

/**
 * Singleton class for cached features
 */
public class FeatureCache {
	private static FeatureCache instance = null;
	private static Object mutex = new Object();

	private ArrayList<String> invalidCFDs;
	private String cfdName;
	/**
	 * Maps cfd name to its XML path.
	 */
	private HashMap<String,String> cfdPathMap;
	private File cacheBaseDir;
	
	private FeatureCache() {
		cacheBaseDir = new File(JSANConstants.JSAN_CACHE_PREFIX);
		if (!cacheBaseDir.exists() || !cacheBaseDir.isDirectory()) {
			try {
				cacheBaseDir.mkdir();
			} catch(SecurityException e) {
				Logger.logln("Failed to create directory "+cacheBaseDir.getAbsolutePath());
			}
		}
	}
	
	public static FeatureCache getInstance() {
		if (instance == null) {
			synchronized (mutex){
				if (instance == null)
					instance = new FeatureCache();
			}
		}
		return instance;
	}
	
	public void invalidate() {
		invalidCFDs.add(cfdName);
	}
	
	public FeatureCache reset() {
		invalidCFDs.clear();
		return this;
	}
	
	/**
	 * Revalidates the CFD, given the path to its XML file.
	 * Also switches to this new features set.
	 * @param cfdName
	 * @param cfdPath
	 * @return
	 */
	public FeatureCache revalidate(String cfdName, String cfdPath) {
		this.cfdName = cfdName;
		invalidCFDs.remove(cfdName);
		cfdPathMap.put(cfdName, cfdPath);
		return this;
	}
	
	/**
	 * Whether or not the cache should be used. If the cache becomes invalid,
	 * but we don't want to delete the cache (say, the user changes the in-memory
	 * CFD, but the old XML file it pointed to is unchanged), we can temporarily
	 * switch off caching for it.
	 * @return
	 */
	public boolean useCache() {
		return !invalidCFDs.contains(cfdName);
	}
	
	/**
	 * Start tracking the feature set with the given name and path.
	 * Also switches to this new feature set.
	 * @param name
	 * @param path
	 * @return this
	 */
	public FeatureCache trackFeatureSet(String name, String path) {
		cfdName = name;
		cfdPathMap.put(name, path);
		return this;
	}
	
	public FeatureCache switchFeatureSet(String name) {
		cfdName = name;
		if (!cfdPathMap.containsKey(name)) {
			Logger.logln("No path known for cfd: " + name);
			invalidate();
		}
		return this;
	}
	
//	public FeatureCache setDoc(String docName, String docAuthor, String docPath) {
//		this.docTitle = docName;
//		this.docAuthor = docAuthor;
//		this.docPath = docPath;
//		return this;
//	}
	
	/**
	 * Loads cached features (before any cullers are applied)
	 * @param doc
	 * @return
	 */
	public List<EventSet> load(Document doc) {
		// LOAD THE CACHE FOR THE GIVEN DOCUMENT
		// As we iterate over the features, we will see if it is in the cache and 
		// add it to esl as we find them. We must make sure it only loads from the
		// cache when it is using the same feature set. How can we determine that
		// the feature set being used is the same? If we load a feature set from XML,
		// and do not modify it, we can store the name of the loaded feature set
		// and use that as a sub-directory of our cache directory. Inside,
		// we determine if the cache is out-dated (if the feature_set has since been
		// modified). If so, clear the cache (delete all the sub-directories inside).
		// The good thing about this is, since they are being loaded here, they are
		// loaded before any cullers are applied, so the caches do not have to reflect
		// that. It would be beneficial to load from the cache before any 
		// canonicizers are applied as well, then store to the cache after completion.
		// Each author gets his/her own directory within the feature-set directory.
		
		// Worrying about an out-dated feature_set is not the only problem.
		// We must keep a file in the directory of each author, specifying the time
		// each document within was last modified. ms since the epoch is fine.
		// If a document is renamed, it would not find the cached features for that
		// document and would scrap that document's cache.
		// If a document is modified, it would scrap the cache as well.
		// We also need to include a path to the file, so that it knows where to
		// look to see when it was last modified.
		
		// ALSO, it is necessary to keep a flag checking if the feature set was
		// modified by the user in any way after loading it. If so, then the
		// cache should be assumed to be invalid. Though this does not stop the
		// user from modifying it outside the program, the time stamp should
		// invalidate it when that is the case. In the case where it is invalidated
		// via API calls, the cache should not be destroyed. If the user saves over
		// the feature_set xml file, its "last modified" will change, and the invalid
		// cache will be removed then. The flag will signal to not use the cache though.
		
		// It may be beneficial to have the object creation (for each EventSet)
		// done in a BackgroundWorker as it reads through a text file.
		
		// So what should the cache folder look like? I figure,
		// WORKING_DIR/jsan_resources/cache/[feature_set]/[author_a]/a_01.cache
		//                                                          /a_02.cache
		//                                               /last_modified.txt
		
		// the *.cache files in the author directory would look something like:
	
		// C:/some/path/to/the/document/a_01.txt
		// 1275265349422
		// Unique Words Count	[EventSet ID]
		// Unique-Words-Count{268}
		// ,
		// Sentence Count
		// Sentence-Count{30}
		// ,
		// Top 25 NGrams
		// Top-25-NGrams{th}
		// Top-25-NGrams{at}
		// Top-25-NGrams{th}
		// ...
		
		// Where the first line is the path to the file, and
		// the second line is the time the document was last modified.
		
		if (!useCache())
			return null;
		try {
			File cacheDir = new File(JSANConstants.JSAN_CACHE_PREFIX);
			File featureDir = new File(cacheDir, cfdName);
			if (!featureDir.exists())
				return null;
			File authorDir = new File(featureDir, doc.getAuthor());
			if (!authorDir.exists())
				return null;
			File cachedDoc = new File(authorDir, doc.getTitle() + ".cache");
			if (!cachedDoc.exists())
				return null;

			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(cachedDoc));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			List<EventSet> esl = null;
			String filePath = reader.readLine();
			String lastModifiedString = reader.readLine();
			long lastModified = Long.parseLong(lastModifiedString);
			File previousCachedFile = new File(filePath);
			String newFilePath = doc.getFilePath();
			File newFile = new File(newFilePath);
			if (!previousCachedFile.equals(newFile)
					|| lastModified != newFile.lastModified()) {
				reader.close();
				return null;
			}
			esl = new ArrayList<EventSet>();
			String line;
			EventSet es = new EventSet();
			es.setAuthor(doc.getAuthor());
			es.setDocumentName(doc.getTitle());
			String ID = reader.readLine();
			es.setEventSetID(ID);
			while ((line = reader.readLine()) != null) {
				if (line.equals(""))
					continue;
				// extract event set
				if (line.equals(",")) {
					esl.add(es);
					es = new EventSet();
					es.setDocumentName(doc.getTitle());
					ID = reader.readLine();
					es.setEventSetID(ID);
					continue;
				}
				es.addEvent(new Event(line));
			}
			esl.add(es); // the last one is not followed by a
							// comma
			reader.close();
			return esl;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void save(List<EventSet> esl, Document doc) {
		if (!useCache()) 
			return;

		File cacheDir = new File(JSANConstants.JSAN_CACHE_PREFIX);
		File featureDir = new File(cacheDir, cfdName);
		if (!featureDir.exists()) {
			if (!featureDir.mkdir()) {
				Logger.logln("Failed to make directory "
						+ featureDir.getAbsolutePath());
				return;
			}
		}
		File authorDir = new File(featureDir, doc.getAuthor());
		if (!authorDir.exists()) {
			if (!authorDir.mkdir()) {
				Logger.logln("Failed to make directory "
						+ authorDir.getAbsolutePath());
				return;
			}
		}

		StringBuilder builder = new StringBuilder();
		File currentDoc = new File(doc.getFilePath());
		builder.append(currentDoc.getAbsolutePath());
		builder.append('\n');
		builder.append(Long.toString(currentDoc.lastModified()));
		builder.append('\n');
		for (int i = 0; i < esl.size(); i++) {
			if (i != 0) {
				builder.append(",\n");
			}
			EventSet es = esl.get(i);
			builder.append(es.getEventSetID());
			builder.append('\n');
			for (Event e : es) {
				builder.append(e.getEvent());
				builder.append('\n');
			}
		}
		String fileContents = builder.toString();
		File cachedDoc = new File(authorDir, doc.getTitle() + ".cache");
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(cachedDoc));
			writer.write(fileContents);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Recursively delete contents of a directory (if f is a directory),
	 * then delete f.
	 * @param dir 	the file/directory to delete
	 * @return true if f was successfully deleted, false otherwise
	 */
	public static boolean deleteRecursive(File f) {
		if (f.exists()) {
			File[] files = f.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteRecursive(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return f.delete();
	}
	
	/**
	 * Will check to see if the CFD's XML was modified, and if so, it will clear
	 * out the cache for that feature set.
	 */
	public void updateFeaturesCache() {
		// if the CFD has been modified in memory, you wouldn't want to delete
		// the stored cache for the XML version of the CFD. Then that could
		// corrupt it.
		if (!useCache())
			return;
		
		// Feature set is the actual XML file for the CFD. Not part of the cache
		File featureSet = null;
		if (cfdPathMap.containsKey(cfdName)) {
			featureSet = new File(cfdPathMap.get(cfdName));
		} 

		if (!cacheBaseDir.exists() || !cacheBaseDir.isDirectory()) {
			try {
				cacheBaseDir.mkdir();
			} catch (SecurityException e) {
				Logger.logln("Failed to create directory "
						+ cacheBaseDir.getAbsolutePath());
				return;
			}
		}
		File cacheDir = new File(cacheBaseDir, cfdName);
		if (!featureSet.exists()) {
			deleteRecursive(cacheDir);
		} else if (!cacheDir.exists()) {
			try {
				if (!cacheDir.mkdir()) {
					Logger.logln("Failed to create directory "
							+ cacheDir.getAbsolutePath());
					return;
				}
			} catch (SecurityException e) {
				Logger.logln("Failed to create directory "
						+ cacheDir.getAbsolutePath());
				return;
			}
		}
		File lastModified = new File(cacheDir, "last_modified");
		if (!lastModified.exists()) {
			BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(
						lastModified));
				writer.write(Long.toString(featureSet.lastModified()));
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			BufferedReader reader = null;
			String cfdLastModified = null;
			try {
				reader = new BufferedReader(new FileReader(
						lastModified));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (reader == null)
				return;
			
			try {
				cfdLastModified = reader.readLine();
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long lmLong = Long.parseLong(cfdLastModified);
			if (lmLong == featureSet.lastModified())
				return;
			// the cache is invalid. clear it out
			File[] cacheDirFiles = cacheDir.listFiles();
			if (cacheDirFiles == null)
				return; // nothing to remove
			for (File f : cacheDirFiles) {
				if (f.equals(lastModified)) {
					BufferedWriter writer = null;
					try {
						writer = new BufferedWriter(new FileWriter(
								lastModified));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (writer == null)
						return;
					try {
						writer.write(Long.toString(featureSet.lastModified()));
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					FileHelper.deleteRecursive(f);
				}
			}
		}

	}
}

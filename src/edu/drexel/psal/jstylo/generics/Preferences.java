package edu.drexel.psal.jstylo.generics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * This is primarily a data storage class which adds some convenience functionality to the program.
 * At the moment, all options in the analysis tab are saved in this data structure, then to a text file, and
 * then loaded upon startup. In other words, it restores the previous run settings whenever this file is present.
 * It also includes several parameters which are used exclusively by the API, either the remote one or the full one.
 * Now will auto select the last used feature set
 * 
 * Functionality to add:
 * 		Recent/previously used directory
 * 		Recent/previously used problem set (maybe)
 */
public class Preferences{

	//older versions will be replaced with the default of the newest version
	private static final double currentVersion = 0.77;
	
	//where the file can be found
	private static final String preferenceFilePath = "./jsan_resources/JStylo_prop.prop";
	
	private static final String classifiersString = 
			"edu.drexel.psal.jstylo.analyzers -F AuthorWPdata.class -F WekaAnalyzer.class -F SynonymBasedClassifier.class<>"+
			"weka.classifiers.bayes<>"+
			"weka.classifiers.functions<>"+
			"weka.classifiers.lazy<>"+
			"weka.classifiers.meta<>"+
			"weka.classifiers.misc<>"+
			"weka.classifiers.rules<>"+
			"weka.classifiers.trees<>";
	
	//keys which are actually worth reading in
	private static final String[] validKeys = {
		"numCalcThreads",
		"useLogFile",
		"useSparse",
		"useDocTitles",
		"loadDocContents",
		"printVectors",
		"calcInfoGain",
		"applyInfoGain",
		"numInfoGain",
		"kFolds",
		"rebuildInstances",
		"analysisType",
		"featureSet",
		"classifiers"};
	
	//Used for default values in the event of a missing/outdated file
	//or when building a Preferences object without a file for internal use
	private static final String defaultPreferenceString = 
			"numCalcThreads=4\n" +
			"useLogFile=0\n" +
			"useSparse=1\n" +
			"useDocTitles=0\n" +
			"loadDocContents=0\n" +
			"printVectors=0\n" +
			"calcInfoGain=1\n" +
			"applyInfoGain=0\n" +
			"numInfoGain=200\n" +
			"kFolds=10\n" +
			"rebuildInstances=0\n" +
			"analysisType=0\n" +
			"featureSet=0\n" +
			"classifiers="+classifiersString+"\n";
	
	//the main data structure
	private Map<String,String> preferences;
	
	/**
	 * An empty Preferences object
	 */
	private Preferences(){
		preferences = new HashMap<String,String>();
	}
	
	/**
	 * A preferences object with default values
	 * @return
	 */
	public static Preferences buildDefaultPreferences(){
		Preferences p = new Preferences();
		for (String s : defaultPreferenceString.split("\n")){
			String[] components = s.split("=");
			p.setPreference(components[0],components[1]);
		}
		return p;
	}
	
	/**
	 * Adds a preference to the map
	 * @param key
	 * @param value
	 */
	public void setPreference(String key,String value){
		boolean isValid = false;
		for (String s : validKeys){
			
			if (s.equals(key)){
				isValid = true;
				break;
			}
		}
		
		if (isValid){
			preferences.put(key, value);
		}
	}
	
	/**
	 * Gets a specific preference from the object
	 * @param key
	 * @return
	 */
	public String getPreference(String key){
		if (preferences.containsKey(key)){
			return preferences.get(key);
		} else {
			return "<Key Not Found>";
		}
	}
	
	/**
	 * Translates a preference into a boolean value (only works on preferences with potential values of 1 and 0
	 * @param key
	 * @return
	 */
	public boolean getBoolPreference(String key){
		if (preferences.containsKey(key)){
			String s = preferences.get(key);
			if (s.equals("0"))
				return false;
			else
				return true;
		} else {
			System.out.println("Invalid key! "+key);
			return false;
		}
	}
	
	/**
	 * Checks to see if the preference file is present
	 * @return
	 */
	public static boolean preferenceFileIsPresent(){
		File f = new File(preferenceFilePath);
		if (f.exists()){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Reads in the preference file and creates a preference object
	 * @return 
	 */
	public static Preferences loadPreferences(){
		BufferedReader reader = null;
		boolean close = true;
		Preferences p = new Preferences();
		
		if (!preferenceFileIsPresent()){
			System.out.println("Preference file not found! Generating default file...");
			createDefaultPreferenceFile();
			return buildDefaultPreferences();
		}
		
		try {
			reader = new BufferedReader(new FileReader(new File(preferenceFilePath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			String line = reader.readLine();
			while(line != null){
				if (line.startsWith("version")){
					String[] components = line.split("=");
					//System.out.println("Old version: "+components[1]+" New version: "+currentVersion);
					double d = Double.parseDouble(components[1]);
					if (d != currentVersion){
						System.out.println("Outdated preference file! Replacing...");
						reader.close();
						close = false;
						createDefaultPreferenceFile();
						return buildDefaultPreferences();
					}
				} else if (line.contains("=")){
					String[] components = line.split("=");
					//System.out.printf("[0]:%s: [1]:%s:\n",components[0],components[1]);
					p.setPreference(components[0],components[1]);
				}
				line = reader.readLine();;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if (close) {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return p;
	}
	
	/**
	 * Saves the preferences object
	 * @param p
	 */
	public static void savePreferences(Preferences p){
		savePreferences(p.toPreferenceString());
	}
	
	/**
	 * Turns the current preferences into string form
	 * @return
	 */
	public String toPreferenceString(){
		String s = "";
		
		for (String key : preferences.keySet()){
			s+=key+"="+preferences.get(key)+"\n";
		}
		return s;
	}
	
	/**
	 * Saves the current preferences to a text file
	 * @param preferenceString
	 */
	public static void savePreferences(String preferenceString){
		BufferedWriter fileWriter = null;
		String printString = "";
		printString += "#JStylo Preferences\n";
		printString += "version="+currentVersion+"\n";
		printString += preferenceString;
		
		try {
			fileWriter = new BufferedWriter(new FileWriter(new File(preferenceFilePath)));
		} catch (IOException e) {
			System.out.println("Failed to open file writer!");
			e.printStackTrace();
		}
		
		try {
			fileWriter.write(printString);
		} catch (IOException e) {
			System.out.println("Failed to write preference!");
			e.printStackTrace();
		}
		
		try {
			fileWriter.close();
		} catch (IOException e) {
			System.out.println("Failed to close writer!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates the default preferences for the program
	 */
	public static void createDefaultPreferenceFile(){
		File pFile = new File(preferenceFilePath);
		if (pFile.exists()){
			pFile.delete();
		}
		
		savePreferences(defaultPreferenceString);
	}
	
}

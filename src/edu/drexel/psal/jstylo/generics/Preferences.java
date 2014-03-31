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

public class Preferences{

	private static final double currentVersion = 0.3;
	private static final String preferenceFilePath = "./jsan_resources/JStylo_prop.prop";
	private static final String[] validKeys = {"numCalcThreads","useLogFile"};
	private static final String defaultPreferenceString = 
			"numCalcThreads=4\n" +
			"useLogFile=0\n";
	
	private Map<String,String> preference;
	
	private Preferences(){
		preference = new HashMap<String,String>();
	}
	
	public static Preferences buildDefaultPreferences(){
		Preferences p = new Preferences();
		p.setPreference("numCalcThreads","4");
		p.setPreference("useLogFile","0");
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
			preference.put(key, value);
		}
	}
	
	/**
	 * Gets a specific preference from the object
	 * @param key
	 * @return
	 */
	public String getPreference(String key){
		if (preference.containsKey(key)){
			return preference.get(key);
		} else {
			return "<Key Not Found>";
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
			while(reader.ready()){
				String line = reader.readLine();
				if (line.startsWith("version")){
					String[] components = line.split("=");
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
					p.setPreference(components[0],components[1]);
				}
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
		return null;
	}
	
	/**
	 * Saves the current preference
	 * @param preferenceString
	 */
	public static void savePreference(String preferenceString){
		BufferedWriter fileWriter = null;
		String printString = "";
		printString += "#JStylo Preference\n";
		printString += "version="+currentVersion;
		printString += preferenceString;
		
		try {
			fileWriter = new BufferedWriter(new FileWriter(new File(preferenceFilePath)));
		} catch (IOException e) {
			System.out.println("Failed to open file writer!");
			e.printStackTrace();
		}
		
		try {
			fileWriter.write(printString);
			createDefaultPreferenceFile();
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
		
		savePreference(defaultPreferenceString);
	}
	
}

package edu.drexel.psal.jstylo.generics.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.drexel.psal.jstylo.generics.Preferences;

public class PreferencesTest {

	private Preferences testPreferences;
	
	
	@Before
	public void setup(){
		testPreferences = Preferences.buildDefaultPreferences();
		Preferences.savePreferences(testPreferences);
		Preferences.createDefaultPreferenceFile();
	}
	
	@Test
	public void getPreference_hasKey_Success() {
		String expectedResult = "4";
		assertEquals(expectedResult,testPreferences.getPreference("numCalcThreads"));
	}
	
	@Test
	public void getPreference_hasNoKey_Success() {
		String expectedResult = "<Key Not Found>";
		assertEquals(expectedResult,testPreferences.getPreference("notExist"));
	}

	
	@Test
	public void getBoolPreference_ValueNotZero_Success(){
		assertTrue(testPreferences.getBoolPreference("numCalcThreads"));
	}
	
	@Test
	public void getBoolPreference_ValueIsZero_Success(){
		assertFalse(testPreferences.getBoolPreference("rebuildInstances"));
	}
	
	@Test
	public void getBoolPreference_KeyNotExist_Success(){
		assertFalse(testPreferences.getBoolPreference("KeyNotExist"));
	}
	
	@Test
	public void preferenceFileIsPresent_Test_Success(){
		assertTrue(testPreferences.preferenceFileIsPresent());
	}
	
	@Test
	public void loadPreferences_Test_Success(){
		testPreferences.equals(testPreferences.loadPreferences());
	}
	
	
}

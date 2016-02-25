package edu.drexel.psal.jstylo.canonicizers;

import java.util.Scanner;
import com.jgaap.generics.Canonicizer;

/** 
 * Extracts POS-tags from the brown corpus.
 * 
 * @author Ariel Stolerman
 */
public class BrownExtractPOSTags extends Canonicizer {

	private static final long serialVersionUID = 1L;

	@Override
	public String displayName(){
		return "Brown Corpus - extract POS tags";
	}

	@Override
	public String tooltipText(){
		return "Extract all POS tags from the Brown corpus documents.";
	}
	
	@Override
	public boolean showInGUI(){
		return true;
	}

	/**
	 * Extract only POS tags (omit words) from the Brown corpus documents.
	 * @param procText Array of characters to be processed.
	 * @return Array of processed characters.
	 */
	@Override
	public char[] process(char[] procText) {
		String procString = new String(procText);
		Scanner scan = new Scanner(procString);
		String resString = "";
		while (scan.hasNext()) {
			String line = scan.nextLine();
			resString += line.replaceAll("\\S+/", "")+"\n";
		}
		resString = resString.toUpperCase();
		scan.close();
		return resString.toCharArray();
	}
}

package edu.drexel.psal.jstylo.eventDrivers;

import com.jgaap.generics.*;
import com.jgaap.generics.Document;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger; 

import java.io.*;
import java.util.*;

/**
 * This changes words into their parts of speech in a document, based on Stanford's MaxentTagger.
 * 
 * @author Ariel Stolerman
 */

public class MaxentPOSNGramsEventDriverGeneric extends EventDriver implements StanfordDriver{
	private static final long serialVersionUID = 1L;
	@Override
	public String displayName() {
		return "Maxent POS N-Grams";
	}

	@Override
	public String tooltipText() {
		return "Stanford Log-linear Part-Of-Speech Tagger for POS N-grams";
	}

	@Override
	public boolean showInGUI() {
		return false;
	}

	public static MaxentTagger tagger = null;
	protected static String taggerPath = null;
	
	public static String getTaggerPath() {
		return taggerPath;
	}

	public static void setTaggerPath(String taggerPath) {
		MaxentPOSTagsEventDriver.taggerPath = taggerPath;
	}
	
	@SuppressWarnings("static-access")
	@Override
	public EventSet createEventSet(Document doc) {
		EventSet es = new EventSet(doc.getAuthor());
		char[] text = doc.getProcessedText();
		String stringText = new String(text);

		// use MaxentPOSTagsEventDriver's tagger
		// initialize tagger and return empty event set if encountered a problem
		if (tagger == null) {
			tagger = initTagger();
			if (tagger == null) return es;
		}
		

		List<List<HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(stringText)));
		ArrayList<TaggedWord> tagged = new ArrayList<TaggedWord>();
		for (List<HasWord> sentence : sentences)
			tagged.addAll(tagger.tagSentence(sentence));
		
		int i,j,n;
		try {
			n = Integer.parseInt(getParameter("N"));
		} catch (NumberFormatException e) {
			n = 2;
		}
		String curr;
		for (i=0; i<tagged.size()-n+1; i++) {
			
			curr = "("+tagged.get(i).tag()+")";
			for (j=1; j<n; j++) {
				
				curr += "-("+tagged.get(i+j).tag()+")";
			}
			es.addEvent(new Event(curr));
		}
		
		sentences.clear();
		sentences = null;
		return es;
	}
	/**
	 * Initialize the tagger.
	 * @return
	 */
	public  MaxentTagger initTagger() {
		MaxentTagger t = null;
		try {
			
			if(taggerPath==null)
			{
				String currentTaggerPath = ""+getClass().getClassLoader().getResource(getParameter("taggerPath"));//getParameter("taggerPath");
			
				if(currentTaggerPath!=null||currentTaggerPath!="")
					taggerPath = currentTaggerPath;
				
			}
			t = new MaxentTagger(taggerPath);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return t;
	}
	
	public void destroyTagger() { 
		taggerPath = null;
		tagger = null;
	}
	
}

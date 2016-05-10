package edu.drexel.psal.jstylo.eventDrivers;

/**
 * A hacky interface to link the stanford drivers which need special cleanup to properly destroy their taggers in order to solve
 * a memory leak issue.
 * @author Travis Dutko  
 * */
public interface StanfordDriver {

	public void destroyTagger();
	
}

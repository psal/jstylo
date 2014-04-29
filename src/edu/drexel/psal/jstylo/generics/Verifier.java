package edu.drexel.psal.jstylo.generics;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;

public abstract class Verifier {

	protected Classifier classifier;
	protected Evaluation eval;
	protected ProblemSet ps;
	protected CumulativeFeatureDriver cfd;
	
	public Verifier(ProblemSet p, Classifier c, Evaluation e, CumulativeFeatureDriver cf){
		classifier = c;
		eval = e;
		ps = p;
		cfd = cf;
	}
	
	public abstract void verify();
	
	public abstract String getResultString();
	
}

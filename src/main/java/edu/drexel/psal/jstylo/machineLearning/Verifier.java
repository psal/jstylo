package edu.drexel.psal.jstylo.machineLearning;

import java.io.Serializable;


public abstract class Verifier implements Serializable{

	private static final long serialVersionUID = 1L;

	public abstract void verify();
	
	public abstract String getResultString();
	
	public abstract double getAccuracy();
	
}

package edu.drexel.psal.jstylo.verifiers;
import java.util.List;

import weka.classifiers.*;
import weka.core.*;
public class DistractorlessClassifier implements Classifier {

	private Instances trainingInstances;
	private double modifier;
	private DistractorlessVerifier dv;
	
	//Do not use for this hacky class
	public DistractorlessClassifier(){
		trainingInstances = null;
		modifier = 0.0;
	}
	
	@Override
	public double classifyInstance(Instance arg0) throws Exception {
		dv = null;
		Instances test = new Instances((Instances)null);
		test.add(arg0);
		dv = new DistractorlessVerifier(trainingInstances,test,modifier,false);
		dv.verify();
		return 0;
	}

	public void classifyInstances(Instances insts){
		dv = null;
		dv = new DistractorlessVerifier(trainingInstances,insts,modifier,false);
		dv.verify();
	}
	
	@Override
	public double[] distributionForInstance(Instance arg0) throws Exception {
		//Skip this as well?
		
		return null;
	}

	public List<Evaluation> getEvaluationResults(){
		return dv.getResultEvaluations();
	}
	
	//We're ignoring this for now. 
	@Override
	public Capabilities getCapabilities() {
		return null;
	}

	@Override
	public void buildClassifier(Instances arg0) throws Exception {
		trainingInstances = arg0;
	}

	public void setModifier(double d){
		modifier = d;
	}
	
	public Evaluation getEval(){
		return dv.getResultsEval();
	}
	
}

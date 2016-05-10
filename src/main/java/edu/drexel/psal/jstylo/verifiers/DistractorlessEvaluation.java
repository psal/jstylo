package edu.drexel.psal.jstylo.verifiers;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Evaluation;
import weka.core.Instance;

/**
 * A higher level data class for Distractorless verification.
 * This is used for general code cleanliness, as well as the ability to more conveniently partition verification data
 * when attempting to verify multiple documents.
 * Ideally, this will also help with "verification analysis" by allowing storage of metadata (such as the true author of a document)
 * to aid in the evaluation of a verifier's effectiveness. 
 * @author Travis Dutko
 */
public class DistractorlessEvaluation implements Serializable{

    private static final Logger LOG = LoggerFactory.getLogger(DistractorlessEvaluation.class);
    
    private static final long serialVersionUID = 1L;
    private Evaluation resultEval;
	private Instance testInstance;
	private String trueAuthor;
	private double distance;
	
	/*
	 * Constructors
	 */
	/**
	 * Basic constructor which intializes 
	 * @param author
	 */
	public DistractorlessEvaluation(String author){
		setResultEval(null);
		setTestInstance(null);
		setTrueAuthor(author);
	}
	
	public DistractorlessEvaluation(String author, Instance inst){
		setResultEval(null);
		setTestInstance(inst);
		setTrueAuthor(author);
	}
	
	public DistractorlessEvaluation(){
		this("_Unknown_");
	}

	/*
	 * functions
	 */
	
	public String getResultString() {
		String s = "";
		try {
			s += "<=========[" + testInstance.stringValue(testInstance.attribute(0)) + "]=========>";
			s += /* e.toSummaryString() + "\n" + e.toClassDetailsString() + "\n" + */resultEval.toMatrixString() + "\n";
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		s = s.replaceAll("=== Confusion Matrix ===", "");
		s = s.replaceAll("a = AUTHOR", "Known Documents");
		s = s.replaceAll("b = NOT_AUTHOR", "Unknown Document");
		s = s.replaceAll("<-- classified as", "<-- a = verified as author. b = rejected as author");
		return s;
	}
	
	public int getCorrectlyVerified(String author) { 
		
		//if this = 1, we verified it as the author
		if (resultEval.confusionMatrix()[1][0] == 1){
			//so check if the author is actually the true author
			if (trueAuthor.equals(author)) { //true positive
				return 0;
			} else { //false positive
				return 1;
			}
		//if this = 0, we rejected it as the author
		} else if (resultEval.confusionMatrix()[1][0] == 0){
			if (trueAuthor.equals(author)) { //false negative
				return 2;
			} else { //true negative
				return 3;
			}
		} else { //failure
			LOG.error("Error in meta verification. Disregard meta verification numbers and perform manual analysis.");
			return 4;
		}
			
		
	}
	
	/*
	 * Setters and getters
	 */
	public Evaluation getResultEval() {
		return resultEval;
	}

	public void setResultEval(Evaluation resultEval) {
		this.resultEval = resultEval;
	}

	public String getTrueAuthor() {
		return trueAuthor;
	}

	public void setTrueAuthor(String trueAuthor) {
		this.trueAuthor = trueAuthor;
	}

	public Instance getTestInstance() {
		return testInstance;
	}
	
	public void setTestInstance(Instance inst) {
		testInstance = inst;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
}

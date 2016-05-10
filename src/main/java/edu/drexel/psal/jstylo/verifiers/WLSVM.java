/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    WLSVM.java
 *    Copyright (C) 2005 Yasser EL-Manzalawy
 *
 */

/**
 * We at PSAL have modified this classifier for use with the (current) latest version of WEKA, v3.7.9
 * We have tried to keep our changes and additions to a minimum. It is currently a work in progress.
 */

package edu.drexel.psal.jstylo.verifiers;

/*
 * An implementation of a custom Weka classifier that provides an access to LibSVM.
 * Available at: http://www.cs.iastate.edu/~yasser/wlsvm
 */


import weka.classifiers.Classifier;
import weka.core.*;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.*;
import weka.filters.unsupervised.attribute.*;
import weka.classifiers.*;

import libsvm.*;

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WLSVM extends Classifier implements WeightedInstancesHandler, OptionHandler, Serializable, TechnicalInformationHandler  {
	
    private static final Logger LOG = LoggerFactory.getLogger(WLSVM.class);
    
	protected static final long serialVersionUID = 14172;
	
	protected svm_parameter param; // LibSVM oprions
	
	protected int normalize; // normalize input data
	
	protected svm_problem prob; // LibSVM Problem
	
	protected svm_model model; // LibSVM Model
	
	protected String error_msg;
	
	protected Filter filter = null;
	
	public WLSVM() {
		String[] dummy = {};
		try{
			setOptions(dummy);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** The filter used to make attributes numeric. */
	//protected NominalToBinary m_NominalToBinary = null;
	
	/**
	 * Returns a string describing classifier
	 * 
	 * @return a description suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	
	
	public String globalInfo() {
		return "An implementation of a custom Weka classifier that provides an access to LibSVM."+
		"Available at: http://www.cs.iastate.edu/~yasser/wlsvm";
	}
	
	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options.
	 */
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Enumeration listOptions() {
		
		Vector newVector = new Vector(13);
		
		newVector.addElement(new Option("\t set type of SVM (default 0)\n"
				+ "\t\t 0 = C-SVC\n" + "\t\t 1 = nu-SVC\n"
				+ "\t\t 2 = one-class SVM\n" + "\t\t 3 = epsilon-SVR\n"
				+ "\t\t 4 = nu-SVR", "S", 1, "-S <int>"));
		
		newVector
		.addElement(new Option(
				"\t set type of kernel function (default 2)\n"
				+ "\t\t 0 = linear: u'*v\n"
				+ "\t\t 1 = polynomial: (gamma*u'*v + coef0)^degree\n"
				+ "\t\t 2 = radial basis function: exp(-gamma*|u-v|^2)\n"
				+ "\t\t 3 = sigmoid: tanh(gamma*u'*v + coef0)",
				"K", 1, "-K <int>"));
		
		newVector.addElement(new Option(
				"\t set degree in kernel function (default 3)", "D", 1,
		"-D <int>"));
		
		newVector.addElement(new Option(
				"\t set gamma in kernel function (default 1/k)", "G", 1,
		"-G <double>"));
		
		newVector.addElement(new Option(
				"\t set coef0 in kernel function (default 0)", "R", 1,
		"-R <double>"));
		
		newVector
		.addElement(new Option(
				"\t set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)",
				"C", 1, "-C <double>"));
		
		newVector
		.addElement(new Option(
				"\t set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)",
				"N", 1, "-N <double>"));
		
		newVector.addElement(new Option(
				"\t whether to normalize input data, 0 or 1 (default 0)", "Z",
				1, "-Z"));
		
		newVector
		.addElement(new Option(
				"\t set the epsilon in loss function of epsilon-SVR (default 0.1)",
				"P", 1, "-P <double>"));
		
		newVector.addElement(new Option(
				"\t set cache memory size in MB (default 40)", "M", 1,
		"-M <double>"));
		
		newVector.addElement(new Option(
				"\t set tolerance of termination criterion (default 0.001)",
				"E", 1, "-E <double>"));
		
		newVector.addElement(new Option(
				"\t whether to use the shrinking heuristics, 0 or 1 (default 1)",
				"H", 1, "-H <int>"));
		
		newVector.addElement(new Option(
				"\t whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)",
				"B", 1, "-B <int>"));
		
		newVector.addElement(new Option(
				"\t set the parameters C of class i to weight[i]*C, for C-SVC (default 1)",
				"W", 1, "-W <double>"));
		
		return newVector.elements();
	}
	
	/**
	 * Sets type of SVM (default 0)
	 * 
	 * @param svm_type
	 */
	public void setSVMType(int svm_type) {
		param.svm_type = svm_type;
	}
	
	/**
	 * Gets type of SVM
	 * 
	 * @return
	 */
	
	public int getSVMType() {
		return param.svm_type;
	}
	
	/**
	 * Sets type of kernel function (default 2)
	 * 
	 * @param kernel_type
	 */
	
	public void setKernelType(int kernel_type) {
		param.kernel_type = kernel_type;
	}
	
	/**
	 * Gets type of kernel function
	 * 
	 * @return
	 */
	public int getKernelType() {
		return param.kernel_type;
	}
	
	/**
	 * Sets the degree of the kernel
	 * 
	 * @param degree
	 */
	
	public void setDegree(double degree) {
		param.degree = (new Double(degree)).intValue();
	}
	
	/**
	 * Gets the degree of the kernel
	 * 
	 * @return
	 */
	public double getDegree() {
		return param.degree;
	}
	
	/**
	 * Sets gamma (default = 1/no of attributes)
	 * 
	 * @param gamma
	 */
	public void setGamma(double gamma) {
		param.gamma = gamma;
	}
	
	/**
	 * Gets gamma
	 * 
	 * @return
	 */
	public double getGamma() {
		return param.gamma;
	}
	
	/**
	 * Sets coef (default 0)
	 * 
	 * @param coef0
	 */
	public void setCoef0(double coef0) {
		param.coef0 = coef0;
	}
	
	/**
	 * Gets coef
	 * 
	 * @return
	 */
	public double getCoef0() {
		return param.coef0;
	}
	
	/**
	 * Sets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
	 * 
	 * @param nu
	 */
	public void setNu(double nu) {
		param.nu = nu;
	}
	
	/**
	 * Gets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
	 * 
	 * @return
	 */
	public double getNu() {
		return param.nu;
	}
	
	/**
	 * Sets cache memory size in MB (default 40)
	 * 
	 * @param cache_size
	 */
	public void setCache(double cache_size) {
		param.cache_size = cache_size;
	}
	
	/**
	 * Gets cache memory size in MB
	 * 
	 * @return
	 */
	
	public double getCache() {
		return param.cache_size;
	}
	
	/**
	 * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)
	 * 
	 * @param cost
	 */
	public void setCost(double cost) {
		param.C = cost;
	}
	
	/**
	 * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR
	 * 
	 * @return
	 */
	
	public double getCost() {
		return param.C;
	}
	
	/**
	 * Sets tolerance of termination criterion (default 0.001)
	 * 
	 * @param eps
	 */
	public void setEps(double eps) {
		param.eps = eps;
	}
	
	/**
	 * Gets tolerance of termination criterion
	 * 
	 * @return
	 */
	
	public double getEps() {
		return param.eps;
	}
	
	/**
	 * Sets the epsilon in loss function of epsilon-SVR (default 0.1)
	 * 
	 * @param loss
	 */
	public void setLoss(double loss) {
		param.p = loss;
	}
	
	/**
	 * Gets the epsilon in loss function of epsilon-SVR
	 * 
	 * @return
	 */
	
	public double getLoss() {
		return param.p;
	}
	
	/**
	 * whether to use the shrinking heuristics, 0 or 1 (default 1)
	 * 
	 * @param shrink
	 */
	public void setShrinking(int shrink) {
		param.shrinking = shrink;
	}
	
	/**
	 * whether to use the shrinking heuristics, 0 or 1 (default 1)
	 * 
	 * @return
	 */
	public double getShrinking() {
		return param.shrinking;
	}
	
	/**
	 * whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0) 
	 */
	public int getProbability (){
		return param.probability;
	}
	
	/**
	 * whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)
	 * @param prob
	 */
	public void setProbability (int prob){
		param.probability = prob;
	}
	/**
	 * whether to normalize input data, 0 or 1 (default 0)
	 * 
	 * @param shrink
	 */
	public void setNormalize(int norm) {
		normalize = norm;
	}
	
	/**
	 * whether to normalize input data, 0 or 1 (default 0)
	 * 
	 * @return
	 */
	public int getNormalize() {
		return normalize;
	}
	
	/**
	 * Sets the parameters C of class i to weight[i]*C, for C-SVC (default 1)
	 * 
	 * @param weights
	 */
	public void setWeights(double[] weights) {
		param.nr_weight = weights.length;
		if (param.nr_weight == 0) {
			LOG.info("Zero Weights processed. Default weights will be used");
		}
		
		param.weight_label[0] = -1; // label of first class
		for (int i = 1; i < param.nr_weight; i++)
			param.weight_label[i] = i;
	}
	
	/**
	 * Gets the parameters C of class i to weight[i]*C, for C-SVC (default 1)
	 * 
	 * @return
	 */
	public double[] getWeights() {
		return param.weight;
	}
	
	/**
	 * Sets the WLSVM classifier options
	 *  
	 */
	public void setOptions(String[] options) throws Exception {
		param = new svm_parameter();
		
		String svmtypeString = Utils.getOption('S', options);
		if (svmtypeString.length() != 0) {
			param.svm_type = Integer.parseInt(svmtypeString);
		} else {
			param.svm_type = svm_parameter.C_SVC;
		}
		
		String kerneltypeString = Utils.getOption('K', options);
		if (kerneltypeString.length() != 0) {
			param.kernel_type = Integer.parseInt(kerneltypeString);
		} else {
			param.kernel_type = svm_parameter.RBF;
		}
		
		String degreeString = Utils.getOption('D', options);
		if (degreeString.length() != 0) {
			param.degree = (new Double(degreeString)).intValue();
		} else {
			param.degree = 3;
		}
		
		String gammaString = Utils.getOption('G', options);
		if (gammaString.length() != 0) {
			param.gamma = (new Double(gammaString)).doubleValue();
		} else {
			param.gamma = 0;
		}
		
		String coef0String = Utils.getOption('R', options);
		if (coef0String.length() != 0) {
			param.coef0 = (new Double(coef0String)).doubleValue();
		} else {
			param.coef0 = 0;
		}
		
		String nuString = Utils.getOption('N', options);
		if (nuString.length() != 0) {
			param.nu = (new Double(nuString)).doubleValue();
		} else {
			param.nu = 0.5;
		}
		
		String cacheString = Utils.getOption('M', options);
		if (cacheString.length() != 0) {
			param.cache_size = (new Double(cacheString)).doubleValue();
		} else {
			param.cache_size = 40;
		}
		
		String costString = Utils.getOption('C', options);
		if (costString.length() != 0) {
			param.C = (new Double(costString)).doubleValue();
		} else {
			param.C = 1;
		}
		
		String epsString = Utils.getOption('E', options);
		if (epsString.length() != 0) {
			param.eps = (new Double(epsString)).doubleValue();
		} else {
			param.eps = 1e-3;
		}
		
		String normString = Utils.getOption('Z', options);
		if (normString.length() != 0) {
			normalize = Integer.parseInt(normString);
		} else {
			normalize = 0;
		}
		
		String lossString = Utils.getOption('P', options);
		if (lossString.length() != 0) {
			param.p = (new Double(lossString)).doubleValue();
		} else {
			param.p = 0.1;
		}
		
		String shrinkingString = Utils.getOption('H', options);
		if (shrinkingString.length() != 0) {
			param.shrinking = Integer.parseInt(shrinkingString);
		} else {
			param.shrinking = 1;
		}
		
		String probString = Utils.getOption('B', options);
		if (probString.length() != 0) {
			param.probability = Integer.parseInt(probString);  
		} else {
			param.probability = 0;
		}
		
		String weightsString = Utils.getOption('W', options);
		if (weightsString.length() != 0) {
			StringTokenizer st = new StringTokenizer(weightsString, " ");
			int n_classes = st.countTokens();
			param.weight_label = new int[n_classes];
			param.weight = new double[n_classes];
			
			// get array of doubles from this string                        
			int count = 0;
			while (st.hasMoreTokens()) {                
				param.weight[count++] = atof(st.nextToken());
			}
			param.nr_weight = count;
			param.weight_label[0] = -1; // label of first class
			for (int i = 1; i < count; i++)
				param.weight_label[i] = i;
		} else {
			param.nr_weight = 0;
			param.weight_label = new int[0];
			param.weight = new double[0];           
		}
	}
	
	/**
	 * Returns the current WLSVM options
	 */
	
	public String[] getOptions() {
		
		if (param == null) {
			String[] dummy = {};
			try{
				setOptions(dummy);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String[] options = new String[40];
		int current = 0;
		
		options[current++] = "-S";
		options[current++] = "" + param.svm_type;
		options[current++] = "-K";
		options[current++] = "" + param.kernel_type;
		options[current++] = "-D";
		options[current++] = "" + param.degree;
		options[current++] = "-G";
		options[current++] = "" + param.gamma;
		options[current++] = "-R";
		options[current++] = "" + param.coef0;
		options[current++] = "-N";
		options[current++] = "" + param.nu;
		options[current++] = "-M";
		options[current++] = "" + param.cache_size;
		
		options[current++] = "-C";
		options[current++] = "" + param.C;
		options[current++] = "-E";
		options[current++] = "" + param.eps;
		options[current++] = "-P";
		options[current++] = "" + param.p;
		options[current++] = "-H";
		options[current++] = "" + param.shrinking;
		options[current++] = "-B";
		options[current++] = "" + param.probability;
		options[current++] = "-Z";
		options[current++] = "" + normalize;
		
		if (param.nr_weight > 0) {
			options[current++] = "-W";
			
			String weights = new String();
			for (int i = 0; i < param.nr_weight; i++) {
				weights += " " + param.weight[i];
			}
			
			options[current++] = weights.trim();
		}
		
		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}
	
	protected static double atof(String s) {
		return Double.valueOf(s).doubleValue();
	}
	
	protected static int atoi(String s) {
		return Integer.parseInt(s);
	}
	
	/**
	 * Converts an ARFF Instance into a string in the sparse format accepted by
	 * LIBSVM
	 * 
	 * @param instance
	 * @return
	 */
	protected String InstanceToSparse(Instance instance) {
		String line = new String();
		int c = (int) instance.classValue();
		if (c == 0)
			c = -1;
		line = c + " ";
		for (int j = 1; j < instance.numAttributes(); j++) {
			if (j-1 == instance.classIndex()) {				
				continue;
			}
			if (instance.isMissing(j-1)) 
				continue;
			if (instance.value(j - 1) != 0)
				line += " " + j + ":" + instance.value(j - 1);
		}
		// LOG.info(line); 
		return (line + "\n");
	}
	
	/**
	 * converts an ARFF dataset into sparse format
	 * 
	 * @param instances
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Vector DataToSparse(Instances data) {
		Vector sparse = new Vector(data.numInstances() + 1);
		
		for (int i = 0; i < data.numInstances(); i++) { // for each instance
			sparse.add(InstanceToSparse(data.instance(i)));
		}
		return sparse;
	}
	
	
	public double[] distributionForInstance (Instance instance) throws Exception {	
		int svm_type = svm.svm_get_svm_type(model);
		int nr_class = svm.svm_get_nr_class(model);
		int[] labels = new int[nr_class];
		LOG.info("nr_class: "+nr_class);
		double[] prob_estimates = null;
		
		if (param.probability == 1) {
			if (svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR) {
				LOG.error("Do not use distributionForInstance for regression models!");
				return null;
			} else {
				svm.svm_get_labels(model, labels);
				prob_estimates = new double[nr_class];
			}
		}
		
		if (filter != null) {
			filter.input(instance);
			filter.batchFinished();
			instance = filter.output();
		}
		
		String line = InstanceToSparse(instance);
		
		StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
		
		@SuppressWarnings("unused")
		double target = atof(st.nextToken());
		int m = st.countTokens() / 2;
		svm_node[] x = new svm_node[m];
		for (int j = 0; j < m; j++) {
			x[j] = new svm_node();
			x[j].index = atoi(st.nextToken());
			x[j].value = atof(st.nextToken());
		}
		
		double v;
		double[] weka_probs = new double[nr_class];
		if (param.probability == 1 && (svm_type == svm_parameter.C_SVC || svm_type == svm_parameter.NU_SVC)) {           
			v = svm.svm_predict_probability(model, x, prob_estimates);
			
			// Return order of probabilities to canonical weka attribute order
			for (int k=0; k < prob_estimates.length; k++) {
				LOG.info(labels[k] + ":" + prob_estimates[k] + " ");
				if (labels[k] == -1) 
					labels[k] = 0;
				weka_probs[labels[k]] = prob_estimates[k];
			}
			 //LOG.info();
		} else {
			v = svm.svm_predict(model, x);
			if (v == -1) 
				v = 0;
			weka_probs[(int)v] = 1;
			// LOG.info(v);
		}
		
		return weka_probs;                
	}
	
	/**
	 * Builds the model
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void buildClassifier(Instances insts) throws Exception {
		
		if (normalize == 1) {
			filter = new Normalize();
			filter.setInputFormat(insts);
			insts = Filter.useFilter(insts, filter);
		}
		
		Vector sparseData = DataToSparse(insts);
		Vector vy = new Vector();
		Vector vx = new Vector();
		int max_index = 0;
		
		for (int d = 0; d < sparseData.size(); d++) {
			String line = (String) sparseData.get(d);
			
			StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
			
			vy.addElement(st.nextToken());
			int m = st.countTokens() / 2;
			svm_node[] x = new svm_node[m];
			for (int j = 0; j < m; j++) {
				x[j] = new svm_node();
				x[j].index = atoi(st.nextToken());
				x[j].value = atof(st.nextToken());
			}
			if (m > 0)
				max_index = Math.max(max_index, x[m - 1].index);
			vx.addElement(x);
		}
		
		prob = new svm_problem();
		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		for (int i = 0; i < prob.l; i++)
			prob.x[i] = (svm_node[]) vx.elementAt(i);
		prob.y = new double[prob.l];
		for (int i = 0; i < prob.l; i++)
			prob.y[i] = atof((String) vy.elementAt(i));
		
		if (param.gamma == 0)
			param.gamma = 1.0 / max_index;
		
		error_msg = svm.svm_check_parameter(prob, param);
		
		if (error_msg != null) {
			LOG.error("Error: " + error_msg + "\n");
			System.exit(1);
		}
		
		try {
			model = svm.svm_train(prob, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		return "WLSVM Classifier By Yasser EL-Manzalawy";
	}
	
	/**
	 * 
	 * @param argv
	 * @throws Exception
	 */
	/*
	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			LOG.info("Usage: Test <arff file>");
			System.exit(1);
		}
		String dataFile = argv[0];
		
		WLSVM lib = new WLSVM();
		
		String[] ops = { new String("-t"), 
				dataFile, 
				new String("-x"),
				new String("5"), 
				new String("-i"),
				//WLSVM options
				new String("-S"),  
				new String("0"),
				new String("-K"), 
				new String("2"),
				new String("-G"), 
				new String("1"), 
				new String("-C"),
				new String("7"),
				//new String("-B"),    
				//new String("1"),
				new String("-M"), 
				new String("100"),
				//new String("-W"), 
				//new String("1.0 1.0")
		};
		
		LOG.info(Evaluation.evaluateModel(lib, ops));
		
	}
	*/
	@Override
	public double classifyInstance(Instance arg0) throws Exception {
		String[] ops = { new String("-t"),
				new String("-x"),
				new String("5"), 
				new String("-i"),
				//WLSVM options
				new String("-S"),  
				new String("0"),
				new String("-K"), 
				new String("2"),
				new String("-G"), 
				new String("1"), 
				new String("-C"),
				new String("7"),
				//new String("-B"),    
				//new String("1"),
				new String("-M"), 
				new String("100"),
				//new String("-W"), 
				//new String("1.0 1.0")
		};
		String result = Evaluation.evaluateModel(this, ops);
		LOG.info("RESULT: "+result);
		return 0;
	}

	@Override
	public Capabilities getCapabilities() {
		Capabilities c = new Capabilities(filter);
		return c;
	}

	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation stub = new TechnicalInformation(Type.INCOLLECTION);
		stub.setValue(Field.AUTHOR,"PlaceHolder");
		return stub;
	}
	
}

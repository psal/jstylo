package edu.drexel.psal.jstylo.GUI;

import edu.drexel.psal.jstylo.GUI.DocsTabDriver.ExtFilter;
import edu.drexel.psal.jstylo.generics.Analyzer;
import edu.drexel.psal.jstylo.generics.AnalyzerTypeEnum;
import edu.drexel.psal.jstylo.generics.CumulativeFeatureDriver;
import edu.drexel.psal.jstylo.generics.InstancesBuilder;
import edu.drexel.psal.jstylo.generics.Logger;
import edu.drexel.psal.jstylo.generics.Preferences;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import edu.drexel.psal.jstylo.generics.Logger.LogOut;
import edu.drexel.psal.jstylo.generics.FullAPI;
import edu.drexel.psal.jstylo.generics.FullAPI.analysisType;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import com.jgaap.generics.Document;

import weka.classifiers.Evaluation;
import weka.core.Instances;

public class AnalysisTabDriver {

	/*
	 * ====================== Analysis tab listeners ======================
	 */

	/**
	 * Initializes all listeners for the analysis tab.
	 */
	protected static void initListeners(final GUIMain main) {

		// calculate InfoGain checkbox
		// ===========================

		main.analysisCalcInfoGainJCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("Calculate InfoGain checkbox was clicked on the analysis tab.");

				// enable / disable the apply InfoGain option
				boolean selected = main.analysisCalcInfoGainJCheckBox.isSelected();
				if (selected)
					main.setPreference("calcInfoGain", "1");
				else
					main.setPreference("calcInfoGain", "0");
				Logger.logln("Calculate InfoGain option - " + (selected ? "selected" : "unselected"));
				main.analysisApplyInfoGainJCheckBox.setEnabled(selected);
				main.infoGainValueJTextField.setEnabled(selected);
			}
		});

		// apply InfoGain checkbox
		// =======================

		main.analysisApplyInfoGainJCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.logln("Apply InfoGain checkbox was clicked on the analysis tab.");

				// enable / disable apply InfoGain text field
				boolean selected = main.analysisApplyInfoGainJCheckBox.isSelected();
				if (selected)
					main.setPreference("applyInfoGain", "1");
				else
					main.setPreference("applyInfoGain", "0");
				Logger.logln("Apply InfoGain option - " + (selected ? "selected" : "unselected"));
				main.infoGainValueJTextField.setEnabled(selected);
			}
		});

		// rebuild Instances checkbox
		// =======================

		main.analysisRebuildInstancesJCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				boolean selected = main.analysisRebuildInstancesJCheckBox.isSelected();
				if (selected)
					main.setPreference("rebuildInstances", "1");
				else
					main.setPreference("rebuildInstances", "0");
			}
		});

		// output vectors checkbox
		// =======================

		main.analysisOutputFeatureVectorJCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				boolean selected = main.analysisOutputFeatureVectorJCheckBox.isSelected();
				if (selected)
					main.setPreference("printVectors", "1");
				else
					main.setPreference("printVectors", "0");
			}
		});

		// sparse checkbox
		// =======================

		main.analysisSparseInstancesJCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				boolean selected = main.analysisSparseInstancesJCheckBox.isSelected();
				if (selected)
					main.setPreference("useSparse", "1");
				else
					main.setPreference("useSparse", "0");
			}
		});

		// export training to ARFF button
		// ==============================

		main.analysisExportTrainToARFFJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("'Training to ARFF...' button clicked on the analysis tab.");

				// check if not null
				if (main.ib.getTrainingInstances() == null) {
					JOptionPane.showMessageDialog(main, "No analysis completed yet.", "Export Training Features Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// write to ARFF
				JFileChooser save = new JFileChooser(main.defaultLoadSaveDir);
				save.addChoosableFileFilter(new ExtFilter("Attribute-Relation File Format (*.arff)", "arff"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".arff"))
						path += ".arff";
					boolean succeeded = InstancesBuilder.writeToARFF(path, main.ib.getTrainingInstances());
					if (succeeded) {
						Logger.log("Saved training features to arff: " + path);
						main.defaultLoadSaveDir = (new File(path)).getParent();
					} else {
						Logger.logln("Failed opening " + path + " for writing", LogOut.STDERR);
						JOptionPane.showMessageDialog(null, "Failed saving training features into:\n" + path,
								"Export Training Features Failure", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					Logger.logln("Export training features to ARFF canceled");
				}
			}
		});

		// export training to CSV button
		// =============================

		main.analysisExportTrainToCSVJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("'Training to CSV...' button clicked on the analysis tab.");

				// check if not null
				if (main.ib.getTrainingInstances() == null) {
					JOptionPane.showMessageDialog(main, "No analysis completed yet.", "Export Training Features Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// write to CSV
				JFileChooser save = new JFileChooser(main.defaultLoadSaveDir);
				save.addChoosableFileFilter(new ExtFilter("Comma-separated values (*.csv)", "csv"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".csv"))
						path += ".csv";
					boolean succeeded = InstancesBuilder.writeToCSV(path, main.ib.getTrainingInstances());
					if (succeeded) {
						Logger.log("Saved training features to csv: " + path);
						main.defaultLoadSaveDir = (new File(path)).getParent();
					} else {
						Logger.logln("Failed opening " + path + " for writing", LogOut.STDERR);
						JOptionPane.showMessageDialog(null, "Failed saving training features into:\n" + path,
								"Export Training Features Failure", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					Logger.logln("Export training features to CSV canceled");
				}
			}
		});

		// export test to ARFF button
		// ==========================

		main.analysisExportTestToARFFJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("'Test to ARFF...' button clicked on the analysis tab.");

				// check if not null
				if (main.ib.getTestInstances() == null) {
					JOptionPane.showMessageDialog(main, "No analysis with test documents completed yet.",
							"Export Test Features Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// write to ARFF
				JFileChooser save = new JFileChooser(main.defaultLoadSaveDir);
				save.addChoosableFileFilter(new ExtFilter("Attribute-Relation File Format (*.arff)", "arff"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".arff"))
						path += ".arff";
					boolean succeeded = InstancesBuilder.writeToARFF(path, main.ib.getTestInstances());
					if (succeeded) {
						Logger.log("Saved test features to arff: " + path);
						main.defaultLoadSaveDir = (new File(path)).getParent();
					} else {
						Logger.logln("Failed opening " + path + " for writing", LogOut.STDERR);
						JOptionPane.showMessageDialog(null, "Failed saving test features into:\n" + path,
								"Export Test Features Failure", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					Logger.logln("Export training features to ARFF canceled");
				}
			}
		});

		// export test to CSV button
		// =========================

		main.analysisExportTestToCSVJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("'Test to CSV...' button clicked on the analysis tab.");

				// check if not null
				if (main.ib.getTestInstances() == null) {
					JOptionPane.showMessageDialog(main, "No analysis with test documents completed yet.",
							"Export Test Features Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// write to CSV
				JFileChooser save = new JFileChooser(main.defaultLoadSaveDir);
				save.addChoosableFileFilter(new ExtFilter("Comma-separated values (*.csv)", "csv"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".csv"))
						path += ".csv";
					boolean succeeded = InstancesBuilder.writeToCSV(path, main.ib.getTestInstances());
					if (succeeded) {
						Logger.log("Saved test features to csv: " + path);
						main.defaultLoadSaveDir = (new File(path)).getParent();
					} else {
						Logger.logln("Failed opening " + path + " for writing", LogOut.STDERR);
						JOptionPane.showMessageDialog(null, "Failed saving test features into:\n" + path,
								"Export Test Features Failure", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					Logger.logln("Export training features to CSV canceled");
				}
			}
		});

		//
		// Analysis-specific options toggling
		// =====================================
		main.analysisTrainCVJRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				main.setPreference("analysisType", "0");
				Logger.logln("K-Fold radio button selected");

				boolean selected = main.analysisTrainCVJRadioButton.isSelected();
				if (selected) {
					main.analysisKFoldJTextField.setEnabled(true);
					main.analysisRebuildInstancesJCheckBox.setEnabled(true);
					// main.analysisRelaxJTextField.setEnabled(true);
				}
			}
		});

		main.analysisClassTestUnknownJRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				main.setPreference("analysisType", "1");
				Logger.logln("Test and Classify Unknown radio button selected");

				boolean selected = main.analysisClassTestUnknownJRadioButton.isSelected();
				if (selected) {
					main.analysisKFoldJTextField.setEnabled(false);
					main.analysisRebuildInstancesJCheckBox.setEnabled(false);
					// main.analysisRelaxJTextField.setEnabled(false);
				}
			}
		});

		main.analysisClassTestKnownJRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				main.setPreference("analysisType", "2");
				Logger.logln("Test and Classify Known radio button selected");

				boolean selected = main.analysisClassTestKnownJRadioButton.isSelected();
				if (selected) {
					main.analysisKFoldJTextField.setEnabled(false);
					main.analysisRebuildInstancesJCheckBox.setEnabled(false);
					// main.analysisRelaxJTextField.setEnabled(false);
				}
			}
		});

		// run analysis button
		// ===================

		main.analysisRunJButton.addActionListener(new ActionListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.logln("'Run Analysis' button clicked in the analysis tab.");

				// check
				if (main.ps == null || main.ps.getAllTrainDocs().size() == 0) {
					JOptionPane.showMessageDialog(main, "Training corpus not set or empty.", "Run Analysis Error",
							JOptionPane.ERROR_MESSAGE);
					return;

				} else if (main.analysisClassTestUnknownJRadioButton.isSelected() && main.ps.getTestDocs().isEmpty()) {
					JOptionPane.showMessageDialog(main, "Test documents not set.", "Run Analysis Error",
							JOptionPane.ERROR_MESSAGE);
					return;

				} else if (main.cfd == null || main.cfd.numOfFeatureDrivers() == 0) {
					JOptionPane.showMessageDialog(main, "Feature set not set or has no features.",
							"Run Analysis Error", JOptionPane.ERROR_MESSAGE);
					return;

				} else if (main.analyzers.isEmpty()) {
					JOptionPane.showMessageDialog(main, "No classifiers added.", "Run Analysis Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				} else if (main.analysisTrainCVJRadioButton.isSelected()) { // makes sure K and N are in the appropriate range
					int docCount = 0;
					try {
						String kfolds = main.analysisKFoldJTextField.getText();
						String nthreads = main.analysisNThreadJTextField.getText();

						// find out how many documents there are
						Enumeration<DefaultMutableTreeNode> authors = ((DefaultMutableTreeNode) main.trainCorpusJTree
								.getModel().getRoot()).children();
						DefaultMutableTreeNode author;
						while (authors.hasMoreElements()) {
							author = authors.nextElement();
							docCount += author.getChildCount();
						}

						if (Integer.parseInt(kfolds) <= 1 || Integer.parseInt(kfolds) > docCount)
							throw new Exception();

						if (Integer.parseInt(nthreads) < 1 || Integer.parseInt(nthreads) > 50)
							throw new Exception();

					} catch (Exception exc) {
						JOptionPane.showMessageDialog(main,
								"K and N do not have correct values. Both must be integers in the range:\n1<K<="
										+ docCount + "\n1<=N<=50", "Run Analysis Error", JOptionPane.ERROR_MESSAGE);
						Logger.logln(exc.getMessage(), LogOut.STDERR);
						return;
					}
				}

				// lock
				lockUnlock(main, true);

				// if the number of calc threads entered is different then the current stored one, change it
				if (Integer.parseInt(main.analysisNThreadJTextField.getText()) != main.ib.getNumThreads()) {
					main.ib.setNumThreads(Integer.parseInt(main.analysisNThreadJTextField.getText()));
				}

				// start analysis thread
				main.analysisThread = new Thread(new RunAnalysisThread(main));
				main.analysisThread.start();
			}
		});

		// stop analysis button
		// ====================

		main.analysisStopJButton.addActionListener(new ActionListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.logln("'Stop' button clicked in the analysis tab.");

				// confirm
				int answer = JOptionPane.showConfirmDialog(main, "Are you sure you want to abort analysis?",
						"Stop Analysis", JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					// stop run and update
					Logger.logln("Stopping analysis");
					main.ib.reset();
					main.analysisThread.stop();
					lockUnlock(main, false);
				}
			}
		});

		// save results button
		// ===================

		main.analysisSaveResultsJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Logger.logln("'Save Results...' button clicked on the analysis tab.");

				// check there are results
				if (main.analysisResultsJTabbedPane.getTabCount() == 0) {
					JOptionPane.showMessageDialog(main, "No results available to save.", "Save Analysis Results Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// write to text file
				JFileChooser save = new JFileChooser(main.defaultLoadSaveDir);
				save.addChoosableFileFilter(new ExtFilter("Text files (*.txt)", "txt"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".txt"))
						path += ".txt";

					BufferedWriter bw = null;
					try {
						int selected = main.analysisResultsJTabbedPane.getSelectedIndex();
						bw = new BufferedWriter(new FileWriter(path));
						bw.write(main.results.get(selected));
						bw.flush();
						bw.close();
						main.defaultLoadSaveDir = (new File(path)).getParent();
					} catch (Exception e) {
						Logger.logln("Failed opening " + path + " for writing", LogOut.STDERR);
						JOptionPane.showMessageDialog(null, "Failed saving analysis results into:\n" + path,
								"Save Analysis Results Failure", JOptionPane.ERROR_MESSAGE);
						if (bw != null) {
							try {
								bw.close();
							} catch (Exception e2) {
							}
						}
						return;
					}

					Logger.log("Saved analysis results: " + path);

				} else {
					Logger.logln("Export analysis results canceled");
				}
			}
		});

		// remove results button
		// =====================
		main.analysisRemoveResultTabJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				Logger.logln("'Remove Result Tab' button clicked on the analysis tab.");
				int i = main.analysisResultsJTabbedPane.getSelectedIndex();
				if (i != -1) {
					main.analysisResultsJTabbedPane.remove(i);
				} else {
					JOptionPane.showMessageDialog(null, "There are no tabs which can be removed.",
							"Remove Result Tab Failure", JOptionPane.ERROR_MESSAGE);
				}
			}

		});

		// about button
		// ============

		main.analysisAboutJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				GUIUpdateInterface.showAbout(main);
			}
		});

		// back button
		// ===========

		main.analysisBackJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Logger.logln("'Back' button clicked in the analysis tab");
				main.mainJTabbedPane.setSelectedIndex(2);
			}
		});
	}

	/**
	 * Returns the timestamp when called.
	 */
	protected static String getTimestamp() {
		SimpleDateFormat tf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		return tf.format(cal.getTime());
	}

	/**
	 * Locks / unlocks analysis tab during analysis / when done or stop is clicked.
	 */
	protected static void lockUnlock(GUIMain main, boolean lock) {
		// tabbed pane
		main.mainJTabbedPane.setEnabled(!lock);

		// all action buttons
		main.analysisClassTestUnknownJRadioButton.setEnabled(!lock);
		main.analysisTrainCVJRadioButton.setEnabled(!lock);
		main.analysisClassTestKnownJRadioButton.setEnabled(!lock);

		// main.analysisOutputAccByClassJCheckBox.setEnabled(!lock);
		// main.analysisOutputConfusionMatrixJCheckBox.setEnabled(!lock);
		main.analysisOutputFeatureVectorJCheckBox.setEnabled(!lock);
		main.analysisSparseInstancesJCheckBox.setEnabled(!lock);
		main.analysisCalcInfoGainJCheckBox.setEnabled(!lock);
		main.analysisApplyInfoGainJCheckBox.setEnabled(!lock);
		main.infoGainValueJTextField.setEnabled(!lock);

		if (main.analysisTrainCVJRadioButton.isSelected()) {
			main.analysisKFoldJTextField.setEnabled(!lock);
			main.analysisRebuildInstancesJCheckBox.setEnabled(!lock);
			// main.analysisRelaxJTextField.setEnabled(!lock);
		}

		main.analysisRebuildInstancesJLabel.setEnabled(!lock);
		// main.analysisRelaxJLabel.setEnabled(!lock);
		main.analysisKFoldJLabel.setEnabled(!lock);
		main.analysisNThreadJLabel.setEnabled(!lock);
		main.analysisNThreadJTextField.setEnabled(!lock);

		main.analysisExportTrainToARFFJButton.setEnabled(!lock);
		main.analysisExportTestToARFFJButton.setEnabled(!lock);
		main.analysisExportTrainToCSVJButton.setEnabled(!lock);
		main.analysisExportTestToCSVJButton.setEnabled(!lock);

		main.analysisRunJButton.setEnabled(!lock);
		main.analysisStopJButton.setEnabled(lock);
		main.analysisRemoveResultTabJButton.setEnabled(!lock);

		main.analysisSaveResultsJButton.setEnabled(!lock);

		// progress bar
		main.analysisJProgressBar.setIndeterminate(lock);

		// back button
		main.analysisBackJButton.setEnabled(!lock);

	}

	/*
	 * ========================================== Main thread class for running the analysis ==========================================
	 */

	public static class RunAnalysisThread implements Runnable {

		protected GUIMain main;
		protected JScrollPane scrollPane;
		protected JTextArea contentJTextArea;
		protected String content;

		public RunAnalysisThread(GUIMain main) {
			this.main = main;
		}

		@SuppressWarnings({ "unchecked", "deprecation" })
		public void run() {
			Logger.logln(">>> Run Analysis thread started.");

			// initialize results tab
			JPanel tab = new JPanel(new BorderLayout());
			main.analysisResultsJTabbedPane.addTab(getTimestamp(), tab);
			main.analysisResultsJTabbedPane.setSelectedIndex(main.analysisResultsJTabbedPane.getTabCount() - 1);
			scrollPane = new JScrollPane();
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			tab.add(scrollPane);
			contentJTextArea = new JTextArea();
			contentJTextArea.setFont(new Font("Courier New", 0, 12));
			scrollPane.setViewportView(contentJTextArea);
			contentJTextArea.setEditable(false);
			content = "";
			boolean classifyTestDocs = main.analysisClassTestUnknownJRadioButton.isSelected()
					|| main.analysisClassTestKnownJRadioButton.isSelected();

			/*
			 * If we're doing a cross validation and rebuilding instances, we have to "fake" removing the train bias by reconstructing/extracting the
			 * features for each fold.
			 * 
			 * Why? The way Weka works for cross validation, you just rotate through divisions of documents. This makes sense. However, the way
			 * features are extracted causes a problem with regards to culling. Culled features are kept on the basis of commonality in the training
			 * data. During cross-validation, this commonality results in a larger-potential-connection between training and testing data not present
			 * in a traditional train/test scenario. Thus, this way is more accurate/indicative of reality, but at the same time considerably slower.
			 * 
			 * Process To fix this, we re-extract and re-cull the original features, then rotate the documents manually. This means separate
			 * classifications for each fold. As a result, we no longer have all of the advanced statistics that weka gives us. We just get the
			 * cumulative accuracy and determine the overall accuracy from that.
			 */
			if (main.analysisTrainCVJRadioButton.isSelected() && main.analysisRebuildInstancesJCheckBox.isSelected()) {

				// find out how many folds we need
				int numFolds = Integer.parseInt(main.analysisKFoldJTextField.getText());

				// load all of the documents
				List<Document> documents = main.ps.getAllTrainDocs();
				Collections.shuffle(documents);

				// and split them into folds
				int docsPerFold = documents.size() / numFolds;

				// cumulative accuracy starts at 0.0
				double cumulativeAccuracy = 0.0;

				// for each fold
				for (int j = 0; j < numFolds; j++) {

					List<Document> trainDocs = new ArrayList<Document>();
					List<Document> testDocs = new ArrayList<Document>();

					// get the training and testing documents
					if (j == numFolds - 1) {
						trainDocs = documents.subList(0, docsPerFold * j);
						testDocs = documents.subList(docsPerFold * j, documents.size());
					} else if (j == 0) {
						trainDocs = documents.subList(docsPerFold, documents.size());
						testDocs = documents.subList(0, docsPerFold);
					} else {
						List<Document> sublistA = documents.subList(0, docsPerFold * j);
						List<Document> sublistB = documents.subList(docsPerFold * (j + 1) + docsPerFold,
								documents.size());
						trainDocs.addAll(sublistA);
						trainDocs.addAll(sublistB);
						testDocs = documents.subList(docsPerFold * j, docsPerFold * (j + 1));
					}

					// build the problem set with the list of documents
					ProblemSet probSet = new ProblemSet();
					for (Document d : trainDocs) {
						probSet.addTrainDoc(d.getAuthor(), d);
					}
					for (Document d : testDocs) {
						probSet.addTestDoc(d.getAuthor(), d);
					}

					// use a series of API calls to build run the test
					FullAPI jstylo;
					try {
						jstylo = new FullAPI.Builder().cfd(new CumulativeFeatureDriver(main.cfd))
								.classifier(main.analyzers.get(0).getClassifier()).useDocTitles(false).ps(probSet)
								.analysisType(analysisType.TRAIN_TEST_KNOWN)
								.numThreads(Integer.parseInt(main.analysisNThreadJTextField.getText())).build();
						jstylo.prepareInstances();
						jstylo.run();

						// and save the cumulative accuracy for each run
						cumulativeAccuracy += Double.parseDouble(jstylo.getClassificationAccuracy());
						System.out.println("Cumulative acurracy after " + j + " is " + cumulativeAccuracy);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// get the overall accuracy by dividing the cumulative by the #of folds
				cumulativeAccuracy = cumulativeAccuracy / numFolds;
				System.out.println(String.format("\nOverall Accuracy: %.4f \n", cumulativeAccuracy));
				content += String.format("\nOverall Accuracy: %.4f \n", cumulativeAccuracy);
				updateResultsView();

				/*
				 * Otherwise, we're doing a normal train/test or cross validation. Note that this does not currently use API calls. This is to give
				 * finer granularity in terms of progress messages. For the more programmatic API usage, this isn't a concern, but for the GUI it is
				 * likely best to give the user constant updates.
				 */
			} else {

				// update header
				// -------------
				content += "============================ JStylo Analysis Output ============================\n"
						+ "Started analysis on "
						+ getTimestamp()
						+ "\n"
						+ (classifyTestDocs ? "Running test documents classification"
								: "Running k-folds cross validation on training corpus") + "\n" + "\n";

				// training set
				content += "Training corpus:\n";
				Enumeration<DefaultMutableTreeNode> authors = ((DefaultMutableTreeNode) main.trainCorpusJTree
						.getModel().getRoot()).children();
				DefaultMutableTreeNode author;
				while (authors.hasMoreElements()) {
					author = authors.nextElement();
					content += "> " + author.getUserObject().toString() + " (" + author.getChildCount()
							+ " documents)\n";
				}
				content += "\n";

				// test set
				if (classifyTestDocs) {
					content += "Test documents:\n";
					Enumeration<DefaultMutableTreeNode> testAuthors = ((DefaultMutableTreeNode) main.testDocsJTree
							.getModel().getRoot()).children();
					DefaultMutableTreeNode testAuthor;
					while (testAuthors.hasMoreElements()) {
						testAuthor = testAuthors.nextElement();
						content += "> " + testAuthor.getUserObject().toString() + " (" + testAuthor.getChildCount()
								+ " documents)\n";
					}
					content += "\n";
				}

				// feature set
				content += "Feature set: " + main.cfd.getName() + ":\n";
				for (int i = 0; i < main.cfd.numOfFeatureDrivers(); i++) {
					content += "> " + main.cfd.featureDriverAt(i).getName() + "\n";
				}
				content += "\n";

				// classifiers
				content += "Analyzers used:\n";
				for (Analyzer a : main.analyzers) {
					content += "> " + String.format("%-50s", a.getClass().getName()) + "\t"
							+ ClassTabDriver.getOptionsStr(a.getOptions()) + "\n";
				}

				content += "\n" + "================================================================================\n"
						+ "\n";

				// update the content window with all of the experiment information
				contentJTextArea.setText(content);

				// create the instances builder
				InstancesBuilder tempBuilder = new InstancesBuilder(main.ib);
				main.ib.reset();
				main.ib = tempBuilder;
				main.ib.setProblemSet(main.ps);
				main.ib.setLoadDocContents(false);
				try {
					main.ib.setCumulativeFeatureDriver(new CumulativeFeatureDriver(main.cfd));
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				main.ib.setUseSparse(main.analysisSparseInstancesJCheckBox.isSelected());

				// Extract the training information
				content += getTimestamp() + " Extracting features from training corpus ("
						+ (main.ib.isSparse() ? "" : "not ") + "using sparse representation)...\n";
				updateResultsView();
				try {
					main.ib.extractEventsThreaded();
				} catch (Exception e) {
					Logger.logln("Could not extract features from training corpus!", LogOut.STDERR);
					e.printStackTrace();

					JOptionPane.showMessageDialog(main,
							"Could not extract features from training corpus:\n" + e.getMessage() + "\n"
									+ "Aborting analysis.", "Analysis Error", JOptionPane.ERROR_MESSAGE);
					updateBeforeStop();
					Thread.currentThread().stop();
				}
				content += getTimestamp() + " done!\n\n";
				updateResultsView();
				content += getTimestamp() + " Building relevant event set...";
				updateResultsView();

				// Cull the events
				try {
					main.ib.initializeRelevantEvents();
				} catch (Exception e1) {
					Logger.logln("Could not extract relevant events from training corpus!", LogOut.STDERR);
					e1.printStackTrace();

					JOptionPane.showMessageDialog(main, "Could not extract relevant events from training corpus:\n"
							+ e1.getMessage() + "\n" + "Aborting analysis.", "Analysis Error",
							JOptionPane.ERROR_MESSAGE);
					updateBeforeStop();
					Thread.currentThread().stop();
				}
				content += getTimestamp() + " done!\n\n";
				updateResultsView();
				content += getTimestamp() + " Building attributes list...";
				updateResultsView();

				// build an attributes list from the events
				try {
					main.ib.initializeAttributes();
				} catch (Exception e1) {
					Logger.logln("Could not create attributes from training corpus!", LogOut.STDERR);
					e1.printStackTrace();

					JOptionPane.showMessageDialog(main,
							"Could not create attributes from training corpus:\n" + e1.getMessage() + "\n"
									+ "Aborting analysis.", "Analysis Error", JOptionPane.ERROR_MESSAGE);
					updateBeforeStop();
					Thread.currentThread().stop();
				}
				content += getTimestamp() + " done!\n\n";
				updateResultsView();
				content += getTimestamp() + " Creating training instances...";
				updateResultsView();

				// build the instances from the attributes
				try {
					main.ib.createTrainingInstancesThreaded();
				} catch (Exception e) {
					Logger.logln("Could not create instances from training corpus!", LogOut.STDERR);
					e.printStackTrace();

					JOptionPane.showMessageDialog(main,
							"Could not create instances from training corpus:\n" + e.getMessage() + "\n"
									+ "Aborting analysis.", "Analysis Error", JOptionPane.ERROR_MESSAGE);
					updateBeforeStop();
					Thread.currentThread().stop();
				}

				content += getTimestamp() + " done!\n\n";

				// if we're to print out the feature vectors, do so
				if (main.analysisOutputFeatureVectorJCheckBox.isSelected()) {
					content += "Training corpus features (ARFF):\n" + "================================\n"
							+ main.ib.getTrainingInstances().toString() + "\n\n";
					updateResultsView();
				}

				// if we're to extract testing data, do so
				if (classifyTestDocs) {
					Logger.logln("Extracting features from test documents...");

					content += getTimestamp() + " Extracting features from test documents ("
							+ (main.ib.isSparse() ? "" : "not ") + "using sparse representation)...\n";
					updateResultsView();

					//if we're working with known authors, remove the "_Unknown_" author
					if (main.analysisClassTestKnownJRadioButton.isSelected()) {
						main.ib.getProblemSet().removeAuthor("_Unknown_");
					}
					
					//create the instances
					try {
						main.ib.createTestInstancesThreaded();
					} catch (Exception e) {
						Logger.logln("Could not create instances from test documents!", LogOut.STDERR);
						e.printStackTrace();

						JOptionPane.showMessageDialog(main,
								"Could not create instances from test documents:\n" + e.getMessage() + "\n"
										+ "Aborting analysis.", "Analysis Error", JOptionPane.ERROR_MESSAGE);
						updateBeforeStop();
						Thread.currentThread().stop();
					}

					//output the feature vectors if we're supposed to
					content += getTimestamp() + " done!\n\n";
					updateResultsView();
					if (main.analysisOutputFeatureVectorJCheckBox.isSelected()) {
						content += "Test documents features (ARFF):\n" + "===============================\n"
								+ main.ib.getTestInstances().toString() + "\n\n";
						updateResultsView();
					}
				}

				// running InfoGain
				// ================
				if (main.analysisCalcInfoGainJCheckBox.isSelected()) {

					content += "Calculating InfoGain on the training set's features\n";
					content += "===================================================\n";

					//get the infoGain values
					int igValue = -1;
					try {
						igValue = Integer.parseInt(main.infoGainValueJTextField.getText());
						main.setPreference("numInfoGain", "" + igValue);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}

					
					try {
						boolean apply = main.analysisApplyInfoGainJCheckBox.isSelected();
						double[][] infoGain = main.ib.calculateInfoGain();
						
						//if we're applying infoGain to cull the features, do so here
						if (apply) {
							main.ib.applyInfoGain(igValue);
							infoGain = main.ib.calculateInfoGain();
						}
						
						//print out the remaining features and their infoGain values.
						Instances trainingInstances = new Instances(main.ib.getTrainingInstances());
						for (int i = 0; i < infoGain.length; i++) {
							
							//once we hit a "0" infogain value, stop printing the data.
							if (infoGain[i][0]==0){ break; }
							
							int index = (int) Math.round(infoGain[i][1]);
							content += String.format("> %-50s   %f\n", trainingInstances.attribute(index).name(),
									infoGain[i][0]);
						}
						updateResultsView();
					} catch (Exception e) {
						content += "ERROR! Could not calculate InfoGain!\n";
						e.printStackTrace();
					}

					content += "done!\n\n";
					updateResultsView();
				}

				// running classification
				// ======================
				
				/*
				 * Perform a train/test classification on unknown test documents
				 */
				if (main.analysisClassTestUnknownJRadioButton.isSelected()) {

					Logger.logln("Starting training and testing phase...");

					content += getTimestamp() + " Starting training and testing phase...\n";
					content += "\n================================================================================\n\n";

					//initialize values
					Analyzer a;
					int numClass = main.analyzers.size();
					
					//perform the experiment once per analyzer
					for (int i = 0; i < numClass; i++) {
						a = main.analyzers.get(i);
						content += "Running analysis with Analyzer " + (i + 1) + " out of " + numClass + ":\n"
								+ "> Classifier: " + a.getName() + "\n" + "> Options:    "
								+ ClassTabDriver.getOptionsStr(a.getOptions()) + "\n\n";
						
						main.analysisDriver = a;
						
						content += getTimestamp() + " Starting classification...\n";
						Logger.log("Starting classification...\n");
						updateResultsView();

						//perform the actual analysis
						main.analysisDriver.classify(main.ib.getTrainingInstances(),
								main.ib.getTestInstances(), main.ps.getAllTestDocs());

						content += getTimestamp() + " done!\n\n";
						Logger.logln("Done!");
						updateResultsView();

						// print out results
						content += "Results:\n" + "========\n";
						content += main.analysisDriver.getLastStringResults();
						updateResultsView();
					}

				/*
				 * Perform a "normal" cross validation 
				 */
				} else if (main.analysisTrainCVJRadioButton.isSelected()) {

					Logger.logln("Starting training K-folds CV phase...");

					content += getTimestamp() + " Starting K-folds cross-validation on training corpus phase...\n";
					content += "\n================================================================================\n\n";

					Analyzer a;
					int numClass = main.analyzers.size();
					
					//once again, perform the experiment for each analyzer we have
					for (int i = 0; i < numClass; i++) {
						a = (Analyzer) main.analyzers.get(i);
						content += "Running analysis with classifier " + (i + 1) + " out of " + numClass + ":\n"
								+ "> Classifier: " + a.getName() + "\n" + "> Options:    "
								+ ClassTabDriver.getOptionsStr(a.getOptions()) + "\n\n";

						main.analysisDriver = a;

						content += getTimestamp() + " Starting cross validation...\n";
						Logger.log("Starting cross validation...");
						updateResultsView();
						// run the experiment
						Object results = main.analysisDriver.runCrossValidation(main.ib.getTrainingInstances(),
								Integer.parseInt(main.analysisKFoldJTextField.getText()), 0, 0);
						main.setPreference("kFolds", main.analysisKFoldJTextField.getText());

						content += getTimestamp() + " done!\n\n";
						Logger.logln("Done!");
						updateResultsView();

						if (results == null) {
							content += "Classifier not working for this feature set, relaxation factor, or similar variable. Please stop the analysis.";
						}
						updateResultsView();

						// print out results
						Evaluation eval = (Evaluation) results;
						content += eval.toSummaryString(false) + "\n";
						try {
							content += eval.toClassDetailsString() + "\n" + eval.toMatrixString() + "\n";
						} catch (Exception e) {
							e.printStackTrace();
						}

						updateResultsView();
					}

				/*
				 * Perform a train test on known authors
				 */
				} else {
					Logger.logln("Starting training and testing phase...");

					content += getTimestamp() + " Starting training and testing phase...\n";
					content += "\n================================================================================\n\n";

					Analyzer a;
					int numClass = main.analyzers.size();
					for (int i = 0; i < numClass; i++) {
						a = main.analyzers.get(i);
						content += "Running analysis with Analyzer " + (i + 1) + " out of " + numClass + ":\n"
								+ "> Classifier: " + a.getName() + "\n" + "> Options:    "
								+ ClassTabDriver.getOptionsStr(a.getOptions()) + "\n\n";

						main.analysisDriver = a;

						//ick another instanceof. See if there's a way around using it.
						if (a.isType(AnalyzerTypeEnum.WRITEPRINTS_ANALYZER)) {
							a.classify(main.ib.getTrainingInstances(), main.ib.getTestInstances(),
									main.ps.getAllTestDocs());
						}

						content += getTimestamp() + " Starting classification...\n";
						Logger.log("Starting classification...\n");
						updateResultsView();

						//get the instances
						Instances train = main.ib.getTrainingInstances();
						Instances test = main.ib.getTestInstances();
						test.setClassIndex(test.numAttributes() - 1);

						//create the evaluation
						Evaluation results = null;
						try {
							results = main.analysisDriver.getTrainTestEval(train, test);
						} catch (Exception e) {
							Logger.logln("Failed to build train test eval with known authors!", LogOut.STDERR);
							content += "Failed to build train test eval with known authors!";
							e.printStackTrace();
						}

						content += getTimestamp() + " done!\n\n";
						Logger.logln("Done!");
						updateResultsView();

						// print out results
						content += "Results:\n" + "========\n";

						//print results
						try {
							content += results.toSummaryString() + "\n" + results.toClassDetailsString() + "\n"
									+ results.toMatrixString() + "\n";
						} catch (Exception e) {
							Logger.logln("Failed to build the statistics string!", LogOut.STDERR);
							content += "Failed to build the statistics string!";
							e.printStackTrace();
						}

						updateResultsView();
					}
				}
				main.setPreference("featureSet", "" + main.featuresSetJComboBox.getSelectedIndex());
				Preferences.savePreferences(main.ib.getPreferences());
			}
			// unlock gui and update results
			updateBeforeStop();
			main.results.add(content);

			Logger.logln(">>> Run Analysis thread finished.");
		}

		public void updateBeforeStop() {
			lockUnlock(main, false);
		}

		/**
		 * Updates the current results tab
		 */
		public void updateResultsView() {
			contentJTextArea.setText(content);
			contentJTextArea.setCaretPosition(content.length());
		}
	}
}

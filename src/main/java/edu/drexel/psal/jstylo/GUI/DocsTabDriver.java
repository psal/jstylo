package edu.drexel.psal.jstylo.GUI;

import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.jgaap.generics.Document;

import edu.drexel.psal.jstylo.featureProcessing.ProblemSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocsTabDriver {

    private static final Logger LOG = LoggerFactory.getLogger(DocsTabDriver.class);
    
	/*
	 * ======================= Documents tab listeners =======================
	 */

	/**
	 * Initialize all documents tab listeners.
	 */
	protected static void initListeners(final GUIMain main) {
		// problem set
		// ===========

		// new problem set button
		main.newProblemSetJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'New Problem Set' button clicked on the documents tab");

				int answer = 0;
				// ask if current problem set is not empty
				if (main.ps != null && (main.ps.hasAuthors() || main.ps.hasTestDocs())) {
					answer = JOptionPane.showConfirmDialog(null, "New Problem Set will override current. Continue?",
							"Create New Problem Set", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION);
				}
				if (answer == 0) {
					main.ps = new ProblemSet();
					main.ps.setTrainCorpusName(main.defaultTrainDocsTreeName);
					GUIUpdateInterface.updateProblemSet(main);
				}
			}
		});

		// load problem set button
		main.loadProblemSetJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Load Problem Set' button clicked on the documents tab");

				int answer = 0;
				// ask if current problem set is not empty
				if (main.ps != null && (main.ps.hasAuthors() || main.ps.hasTestDocs())) {
					answer = JOptionPane.showConfirmDialog(null,
							"Loading Problem Set will override current. Continue?", "Load Problem Set",
							JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION);
				}
				if (answer == 0) {
					JFileChooser load = new JFileChooser(new File(main.defaultLoadSaveDir));
					load.addChoosableFileFilter(new ExtFilter("XML files (*.xml)", "xml"));
					answer = load.showOpenDialog(main);

					if (answer == JFileChooser.APPROVE_OPTION) {
						String path = load.getSelectedFile().getPath();
						LOG.info("Trying to load problem set from " + path);
						try {
							main.ps = new ProblemSet(path);
							main.defaultLoadSaveDir = (new File(path)).getParent();
							GUIUpdateInterface.updateProblemSet(main);
						} catch (Exception exc) {
							LOG.error("Failed loading " + path, exc);
							JOptionPane.showMessageDialog(null, "Failed loading problem set from:\n" + path,
									"Load Problem Set Failure", JOptionPane.ERROR_MESSAGE);
						}

					} else {
						LOG.info("Load problem set canceled");
					}
				}
			}
		});

		// save problem set button
		main.saveProblemSetJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Save Problem Set' button clicked on the documents tab.");

				JFileChooser save = new JFileChooser(new File(main.defaultLoadSaveDir));
				save.addChoosableFileFilter(new ExtFilter("XML files (*.xml)", "xml"));
				int answer = save.showSaveDialog(main);

				if (answer == JFileChooser.APPROVE_OPTION) {
					File f = save.getSelectedFile();
					String path = f.getAbsolutePath();
					if (!path.toLowerCase().endsWith(".xml"))
						path += ".xml";
					try {
						BufferedWriter bw = new BufferedWriter(new FileWriter(path));
						bw.write(main.ps.toXMLString());
						bw.flush();
						bw.close();
						main.defaultLoadSaveDir = (new File(path)).getParent();
						LOG.info("Saved problem set to " + path + ":\n" + main.ps.toXMLString());
					} catch (IOException exc) {
						LOG.error("Failed opening " + path + " for writing",exc);
						JOptionPane.showMessageDialog(null, "Failed saving problem set into:\n" + path,
								"Save Problem Set Failure", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					LOG.info("Save problem set canceled");
				}
			}
		});

		// test documents
		// ==============

		// test documents table
		// -- none --

		// add test documents button
		main.addTestDocJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Add Document(s)...' button clicked under the 'Test Docs' section on the documents tab.");

				if (main.testDocsJTree.getSelectionCount() == 0
						|| main.testDocsJTree.getSelectionPath().getPath().length != 2) {
					JOptionPane.showMessageDialog(null, "You must select an author.", "Add Training Documents Error",
							JOptionPane.ERROR_MESSAGE);
					LOG.error("tried to add training documents without selecting an author");

				} else {
					String author = main.testDocsJTree.getSelectionPath().getPath()[1].toString();
					JFileChooser open = new JFileChooser(new File(main.defaultLoadSaveDir));
					open.setMultiSelectionEnabled(true);
					open.addChoosableFileFilter(new ExtFilter("Text files (*.txt)", "txt"));
					int answer = open.showOpenDialog(main);

					if (answer == JFileChooser.APPROVE_OPTION) {
						File[] files = open.getSelectedFiles();
						String msg = "Trying to load training documents for author \"" + author + "\":\n";
						for (File file : files)
							msg += "\t\t> " + file.getPath() + "\n";
						LOG.info(msg);

						String path = null;
						String skipList = "";
						ArrayList<String> allTrainDocPaths = new ArrayList<String>();
						ArrayList<String> allTestDocPaths = new ArrayList<String>();

						for (Document doc : main.ps.getAllTrainDocs()) {
							allTrainDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						}
						for (Document doc : main.ps.getAllTestDocs()) {
							allTestDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						}
						for (File file : files) {
							path = file.getPath().replaceAll("\\\\", "/");
							if (allTrainDocPaths.contains(path)) {
								skipList += "\n" + path + " - already listed as a training document for author "
										+ author;
								continue;
							}
							if (allTestDocPaths.contains(path)) {
								skipList += "\n" + path + " - already listed as a test document for author" + author;
								continue;
							}
							main.ps.addTestDoc(author, new Document(path, "dummy", file.getName()));
						}
						if (path != null)
							main.defaultLoadSaveDir = (new File(path)).getParent();

						if (!skipList.equals("")) {
							if (skipList.length() < 3000) {
								JOptionPane.showMessageDialog(null, "Skipped the following documents:" + skipList,
										"Add Testing Documents", JOptionPane.WARNING_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(null, "Skipping too many documents to list...",
										"Add Testing Documents", JOptionPane.WARNING_MESSAGE);
							}
							LOG.info("skipped the following training documents:" + skipList);
						}

						GUIUpdateInterface.updateTestDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);

					} else {
						LOG.info("Load testing documents canceled");
					}
				}
			}
		});

		// remove test documents button
		main.removeTestDocJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Remove Document(s)' button clicked under the 'Test Docs' section on the documents tab.");

				TreePath[] paths = main.testDocsJTree.getSelectionPaths();
				List<DefaultMutableTreeNode> selectedDocs = new ArrayList<DefaultMutableTreeNode>();
				if (paths != null)
					for (TreePath path : paths)
						if (path.getPath().length == 3)
							selectedDocs.add((DefaultMutableTreeNode) path.getPath()[2]);

				if (selectedDocs.isEmpty()) {
					LOG.error("Failed removing training documents - no documents are selected");
					JOptionPane.showMessageDialog(null, "You must select training documents to remove.",
							"Remove Testing Documents Failure", JOptionPane.WARNING_MESSAGE);
				} else {
					int answer = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to remove the selected training documents?",
							"Remove Testing Documents Confirmation", JOptionPane.YES_NO_OPTION);

					if (answer == 0) {
						String msg = "Removed testing documents:\n";
						String author;
						for (DefaultMutableTreeNode doc : selectedDocs) {
							author = doc.getParent().toString();
							main.ps.removeTestDocAt(author, doc.toString());
							msg += "\t\t> " + doc.toString() + "\n";
						}
						LOG.info(msg);
						GUIUpdateInterface.updateTestDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);
					} else {
						LOG.info("Removing testing documents canceled");
					}
				}
			}
		});

		// preview test document button
		main.testDocPreviewJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Preview Document' button clicked under the 'Test Documents' section on the documents tab.");

				TreePath path = main.testDocsJTree.getSelectionPath();
				if (path == null || path.getPathCount() != 3) {
					JOptionPane.showMessageDialog(null,
							"You must select a test document in order to show its preview.",
							"Show Testing Document Preview Error", JOptionPane.ERROR_MESSAGE);
					LOG.error("No testing document is selected for preview");
				} else {
					String docTitle = path.getPath()[2].toString();
					Document doc = main.ps.testDocAt(path.getPath()[1].toString(), docTitle);
					try {
						doc.load();
						main.docPreviewNameJLabel.setText("- " + doc.getTitle());
						main.docPreviewJTextPane.setText(doc.stringify());
					} catch (Exception exc) {
						JOptionPane.showMessageDialog(null,
								"Failed opening testing document for preview:\n" + doc.getFilePath(),
								"Show Testing Document Preview Error", JOptionPane.ERROR_MESSAGE);
						LOG.info("Failed opening testing document for preview", exc);
						GUIUpdateInterface.clearDocPreview(main);
					}
				}
			}
		});

		// add test author button
		main.addTestAuthorJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Add Author...' button clicked under the 'Test Docs' section on the documents tab.");

				String ans = JOptionPane
						.showInputDialog(
								null,
								"Enter new author name or press ok without entering anything\nto select an author folder to read documents from.",
								"", JOptionPane.OK_CANCEL_OPTION);
				if (ans == null) {
					LOG.info("Aborted adding new author");
				} else if (ans.isEmpty()) {

					LOG.info("No author name entered, attempting to load from folder...");

					JFileChooser open = new JFileChooser(new File(main.defaultLoadSaveDir));
					open.setMultiSelectionEnabled(true);
					open.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int answer = open.showOpenDialog(main);

					if (answer == JFileChooser.APPROVE_OPTION) {
						File[] authors = open.getSelectedFiles();
						LOG.info("Trying to load testing documents");

						String path = null;
						String skipList = "";
						ArrayList<String> allTrainDocPaths = new ArrayList<String>();
						ArrayList<String> allTestDocPaths = new ArrayList<String>();

						for (Document doc : main.ps.getAllTrainDocs()) {
							allTrainDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						}
						for (Document doc : main.ps.getAllTestDocs()) {
							allTestDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						}
						for (File dir : authors) { // for each directory selected
							for (File file : dir.listFiles()) { // for each file in that directory
								path = file.getPath().replaceAll("\\\\", "/");
								if (allTrainDocPaths.contains(path)) { // make sure that the file isn't already in the training set
									skipList += "\n" + path + " - already listed as a training document for author "
											+ dir.getName();
									continue;
								}
								if (allTestDocPaths.contains(path)) { // make sure that the file isn't in the test set
									skipList += "\n" + path + " - already listed as a test document";
									continue;
								}

								main.ps.addTestDoc(dir.getName(), new Document(path, dir.getName(), file.getName()));
							}
						}
						if (path != null) // change default directory
							main.defaultLoadSaveDir = ((new File(path)).getParentFile()).getParent();

						if (!skipList.equals("")) { // if there was at least one file that was already present in one of the sets already, inform the
													// user of which one it was
							if (skipList.length() < 3000) {
								JOptionPane.showMessageDialog(null, "Skipped the following documents:" + skipList,
										"Add Testing Documents", JOptionPane.WARNING_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(null, "Skipping too many documents to list...",
										"Add Testing Documents", JOptionPane.WARNING_MESSAGE);
							}
							LOG.info("skipped the following testing documents:" + skipList);
						}

						GUIUpdateInterface.updateTestDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);

					} else {
						LOG.info("Load testing documents canceled");
					}

				} else {
					if (main.ps.getTestAuthorMap().keySet().contains(ans)) {
						JOptionPane.showMessageDialog(null, "Author \"" + ans + "\" already exists.",
								"Add New Author Error", JOptionPane.ERROR_MESSAGE);
						LOG.error("tried to add author that already exists: " + ans);
					} else {
						main.ps.addTestDocs(ans, new ArrayList<Document>());
						GUIUpdateInterface.updateTestDocTree(main);
						LOG.info("Added new author: " + ans);
					}
				}
			}
		});

		// remove testauthor button
		main.removeTestAuthorJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Remove Author(s)' button clicked under the 'Test Docs' section on the documents tab.");

				TreePath[] paths = main.testDocsJTree.getSelectionPaths();
				List<DefaultMutableTreeNode> selectedAuthors = new ArrayList<DefaultMutableTreeNode>();
				if (paths != null)
					for (TreePath path : paths)
						if (path.getPath().length == 2)
							selectedAuthors.add((DefaultMutableTreeNode) path.getPath()[1]);

				if (selectedAuthors.isEmpty()) {
					LOG.error("Failed removing authors - no authors are selected");
					JOptionPane.showMessageDialog(null, "You must select authors to remove.", "Remove Authors Failure",
							JOptionPane.WARNING_MESSAGE);
				} else {
					int answer = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to remove the selected authors?", "Remove Authors Confirmation",
							JOptionPane.YES_NO_OPTION);

					if (answer == 0) {
						String msg = "Removed authors:\n";
						for (DefaultMutableTreeNode author : selectedAuthors) {
							main.ps.removeTestAuthor(author.toString());
							msg += "\t\t> " + author.toString() + "\n";
						}
						LOG.info(msg);
						GUIUpdateInterface.updateTestDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);
					} else {
						LOG.info("Removing authors canceled");
					}
				}
			}
		});

		// training documents
		// ==================

		// training documents tree
		// -- none --

		// add author button
		main.addAuthorJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Add Author...' button clicked under the 'Training Corpus' section on the documents tab.");

				String ans = JOptionPane
						.showInputDialog(
								null,
								"Enter new author name or press ok without entering anything\nto select an author folder to read documents from.",
								"", JOptionPane.OK_CANCEL_OPTION);
				if (ans == null) {
					LOG.info("Aborted adding new author");
				} else if (ans.isEmpty()) {

					LOG.info("No author name entered, attempting to load from folder...");

					JFileChooser open = new JFileChooser(new File(main.defaultLoadSaveDir));
					open.setMultiSelectionEnabled(true);
					open.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int answer = open.showOpenDialog(main);

					if (answer == JFileChooser.APPROVE_OPTION) {
						File[] authors = open.getSelectedFiles();
						LOG.info("Trying to load training documents");

						String path = null;
						String skipList = "";
						ArrayList<String> allTrainDocPaths = new ArrayList<String>();
						ArrayList<String> allTestDocPaths = new ArrayList<String>();

						for (Document doc : main.ps.getAllTrainDocs())
							allTrainDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						for (Document doc : main.ps.getAllTestDocs())
							allTestDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						for (File dir : authors) { // for each directory selected
							for (File file : dir.listFiles()) { // for each file in that directory
								path = file.getPath().replaceAll("\\\\", "/");
								if (allTrainDocPaths.contains(path)) { // make sure that the file isn't already in the training set
									skipList += "\n" + path + " - already contained for author " + dir.getName();
									continue;
								}
								if (allTestDocPaths.contains(path)) { // make sure that the file isn't in the test set
									skipList += "\n" + path + " - already contained as a test document";
									continue;
								}

								main.ps.addTrainDoc(dir.getName(), new Document(path, "dummy", file.getName()));
							}
						}
						if (path != null) // change default directory
							main.defaultLoadSaveDir = ((new File(path)).getParentFile()).getParent();

						if (!skipList.equals("")) { // if there was at least one file that was already present in one of the sets already, inform the
													// user of which one it was
							if (skipList.length() < 3000) {
								JOptionPane.showMessageDialog(null, "Skipped the following documents:" + skipList,
										"Add Training Documents", JOptionPane.WARNING_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(null, "Skipping too many documents to list...",
										"Add Training Documents", JOptionPane.WARNING_MESSAGE);
							}
							LOG.info("skipped the following training documents:" + skipList);
						}

						GUIUpdateInterface.updateTrainDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);

					} else {
						LOG.info("Load training documents canceled");
					}

				} else {
					if (main.ps.getAuthorMap().keySet().contains(ans)) {
						JOptionPane.showMessageDialog(null, "Author \"" + ans + "\" already exists.",
								"Add New Author Error", JOptionPane.ERROR_MESSAGE);
						LOG.error("tried to add author that already exists: " + ans);
					} else {
						main.ps.addTrainDocs(ans, new ArrayList<Document>());
						GUIUpdateInterface.updateTrainDocTree(main);
						LOG.info("Added new author: " + ans);
					}
				}
			}
		});

		// add training documents button
		main.addTrainDocsJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Add Document(s)...' button clicked under the 'Training Corpus' section on the documents tab.");

				if (main.trainCorpusJTree.getSelectionCount() == 0
						|| main.trainCorpusJTree.getSelectionPath().getPath().length != 2) {
					JOptionPane.showMessageDialog(null, "You must select an author.", "Add Training Documents Error",
							JOptionPane.ERROR_MESSAGE);
					LOG.error("tried to add training documents without selecting an author");

				} else {
					String author = main.trainCorpusJTree.getSelectionPath().getPath()[1].toString();
					JFileChooser open = new JFileChooser(new File(main.defaultLoadSaveDir));
					open.setMultiSelectionEnabled(true);
					open.addChoosableFileFilter(new ExtFilter("Text files (*.txt)", "txt"));
					int answer = open.showOpenDialog(main);

					if (answer == JFileChooser.APPROVE_OPTION) {
						File[] files = open.getSelectedFiles();
						String msg = "Trying to load training documents for author \"" + author + "\":\n";
						for (File file : files)
							msg += "\t\t> " + file.getPath() + "\n";
						LOG.info(msg);

						String path = null;
						String skipList = "";
						ArrayList<String> allTrainDocPaths = new ArrayList<String>();
						ArrayList<String> allTestDocPaths = new ArrayList<String>();
						for (Document doc : main.ps.getTrainDocs(author))
							allTrainDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						for (Document doc : main.ps.getAllTestDocs())
							allTestDocPaths.add(doc.getFilePath().replaceAll("\\\\", "/"));
						for (File file : files) {
							path = file.getPath().replaceAll("\\\\", "/");
							if (allTrainDocPaths.contains(path)) {
								skipList += "\n" + path + " - already contained for author " + author;
								continue;
							}
							if (allTestDocPaths.contains(path)) {
								skipList += "\n" + path + " - already contained as a test document";
								continue;
							}
							main.ps.addTrainDoc(author, new Document(path, "dummy", file.getName()));
						}
						if (path != null)
							main.defaultLoadSaveDir = (new File(path)).getParent();

						if (!skipList.equals("")) {
							if (skipList.length() < 5000) {
								JOptionPane.showMessageDialog(null, "Skipped the following documents:" + skipList,
										"Add Training Documents", JOptionPane.WARNING_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(null, "Skipping too many documents to list...",
										"Add Training Documents", JOptionPane.WARNING_MESSAGE);
							}
							LOG.info("skipped the following training documents:" + skipList);
						}

						GUIUpdateInterface.updateTrainDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);

					} else {
						LOG.info("Load training documents canceled");
					}
				}
			}
		});

		// edit corpus name button
		main.trainNameJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Edit Name...' button clicked under the 'Training Corpus' section on the documents tab.");

				String answer = JOptionPane.showInputDialog(null, "Edit corpus name:", main.ps.getTrainCorpusName());
				if (answer == null) {
					LOG.info("Aborted editing corpus name");
				} else if (answer.isEmpty()) {
					JOptionPane.showMessageDialog(null, "Training corpus name must be a non-empty string.",
							"Edit Training Corpus Name Error", JOptionPane.ERROR_MESSAGE);
					LOG.error("tried to change training corpus name to an empty string");
				} else {
					main.ps.setTrainCorpusName(answer);
					GUIUpdateInterface.updateTrainDocTree(main);
				}
			}
		});

		// remove author button
		main.removeAuthorJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Remove Author(s)' button clicked under the 'Training Corpus' section on the documents tab.");

				TreePath[] paths = main.trainCorpusJTree.getSelectionPaths();
				List<DefaultMutableTreeNode> selectedAuthors = new ArrayList<DefaultMutableTreeNode>();
				if (paths != null)
					for (TreePath path : paths)
						if (path.getPath().length == 2)
							selectedAuthors.add((DefaultMutableTreeNode) path.getPath()[1]);

				if (selectedAuthors.isEmpty()) {
					LOG.error("Failed removing authors - no authors are selected");
					JOptionPane.showMessageDialog(null, "You must select authors to remove.", "Remove Authors Failure",
							JOptionPane.WARNING_MESSAGE);
				} else {
					int answer = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to remove the selected authors?", "Remove Authors Confirmation",
							JOptionPane.YES_NO_OPTION);

					if (answer == 0) {
						String msg = "Removed authors:\n";
						for (DefaultMutableTreeNode author : selectedAuthors) {
							main.ps.removeAuthor(author.toString());
							msg += "\t\t> " + author.toString() + "\n";
						}
						LOG.info(msg);
						GUIUpdateInterface.updateTrainDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);
					} else {
						LOG.info("Removing authors canceled");
					}
				}
			}
		});

		// remove training documents button
		main.removeTrainDocsJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Remove Document(s)' button clicked under the 'Training Corpus' section on the documents tab.");

				TreePath[] paths = main.trainCorpusJTree.getSelectionPaths();
				List<DefaultMutableTreeNode> selectedDocs = new ArrayList<DefaultMutableTreeNode>();
				if (paths != null)
					for (TreePath path : paths)
						if (path.getPath().length == 3)
							selectedDocs.add((DefaultMutableTreeNode) path.getPath()[2]);

				if (selectedDocs.isEmpty()) {
					LOG.info("Failed removing training documents - no documents are selected");
					JOptionPane.showMessageDialog(null, "You must select training documents to remove.",
							"Remove Training Documents Failure", JOptionPane.WARNING_MESSAGE);
				} else {
					int answer = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to remove the selected training documents?",
							"Remove Training Documents Confirmation", JOptionPane.YES_NO_OPTION);

					if (answer == 0) {
						String msg = "Removed training documents:\n";
						String author;
						for (DefaultMutableTreeNode doc : selectedDocs) {
							author = doc.getParent().toString();
							main.ps.removeTrainDocAt(author, doc.toString());
							msg += "\t\t> " + doc.toString() + "\n";
						}
						LOG.info(msg);
						GUIUpdateInterface.updateTrainDocTree(main);
						GUIUpdateInterface.clearDocPreview(main);
					} else {
						LOG.info("Removing training documents canceled");
					}
				}
			}
		});

		// preview training document button
		main.trainDocPreviewJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Preview Document' button clicked under the 'Training Corpus' section on the documents tab.");

				TreePath path = main.trainCorpusJTree.getSelectionPath();
				if (path == null || path.getPathCount() != 3) {
					JOptionPane.showMessageDialog(null,
							"You must select a training document in order to show its preview.",
							"Show Training Document Preview Error", JOptionPane.ERROR_MESSAGE);
					LOG.error("No training document is selected for preview");
				} else {
					String docTitle = path.getPath()[2].toString();
					Document doc = main.ps.trainDocAt(path.getPath()[1].toString(), docTitle);
					try {
						doc.load();
						main.docPreviewNameJLabel.setText("- " + doc.getTitle());
						main.docPreviewJTextPane.setText(doc.stringify());
					} catch (Exception exc) {
						JOptionPane.showMessageDialog(null,
								"Failed opening training document for preview:\n" + doc.getFilePath(),
								"Show Training Document Preview Error", JOptionPane.ERROR_MESSAGE);
						LOG.info("Failed opening training document for preview",exc);
						GUIUpdateInterface.clearDocPreview(main);
					}
				}
			}
		});

		// document preview
		// ================

		// document preview clear button
		main.clearDocPreviewJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Clear Preview' button clicked on the documents tab.");

				GUIUpdateInterface.clearDocPreview(main);
			}
		});

		// button toolbar operations
		// =========================

		// about button
		// ============

		main.docsAboutJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				GUIUpdateInterface.showAbout(main);
			}
		});

		// next button
		main.docTabNextJButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LOG.info("'Next' button clicked on the documents tab.");

				if (main.ps == null || !main.ps.hasAuthors() || !main.ps.hasTestDocs()) {
					JOptionPane.showMessageDialog(null,
							"You must set training corpus and test documents before continuing.", "Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					main.mainJTabbedPane.setSelectedIndex(1);
				}
			}
		});
	}

	/*
	 * ===================== Supporting operations =====================
	 */

	/**
	 * Extension File Filter
	 */
	public static class ExtFilter extends FileFilter {

		private String desc;
		private String[] exts;

		// constructors

		public ExtFilter(String desc, String[] exts) {
			this.desc = desc;
			this.exts = exts;
		}

		public ExtFilter(String desc, String ext) {
			this.desc = desc;
			this.exts = new String[] { ext };
		}

		// operations

		@Override
		public String getDescription() {
			return desc;
		}

		@Override
		public boolean accept(File f) {
			if (f.isDirectory())
				return true;
			String path = f.getPath().toLowerCase();
			for (String extension : exts) {
				if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.'))
					return true;
			}
			return false;
		}
	}
}

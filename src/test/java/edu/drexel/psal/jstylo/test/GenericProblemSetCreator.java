package edu.drexel.psal.jstylo.test;

import edu.drexel.psal.jstylo.generics.ProblemSet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author bekahoverdorf
 */
public class GenericProblemSetCreator
{
    private String fileDest;
    private ArrayList<TrainingAuthor> testFiles;
    private ArrayList<TrainingAuthor> trainFiles;

    /**
     * Creates a problem set at the fileWithPath.
     */
    public GenericProblemSetCreator(String fileWithPath)
    {
        if(!fileWithPath.contains("."))
        {
            fileWithPath += ".xml";
        }
        this.fileDest = fileWithPath;
        testFiles = new ArrayList<TrainingAuthor>();
        trainFiles = new ArrayList<TrainingAuthor>();
    }

    /**
     * Creates a problem set with all of the documents in the training set.
     *
     * @param source
     * @param saveLoc
     */
    public static void createProblemSetFromAllFoldersAt(File source, File saveLoc) throws IOException
    {
        GenericProblemSetCreator ps = new GenericProblemSetCreator(saveLoc.getPath());
        // get all of the non-hidden files
        File[] authorDirs = BekahUtil.listNotHiddenFiles(source);

        for(File authorDir : authorDirs)
        {
            TrainingAuthor t;
            t = new TrainingAuthor(authorDir.getName());
            File[] docs = BekahUtil.listNotHiddenFiles(authorDir);
            for(File doc : docs)
            {
                t.addDoc(doc);
            }
            ps.addTrain(t);
        }
        ps.save();
    }

    public static void createProblemSetsWithSize(File source, File saveLoc, int numAuthors, int min_docs) throws IOException
    {
        GenericProblemSetCreator ps = new GenericProblemSetCreator(saveLoc.getPath().replace(".xml", numAuthors + "_" + 0 + ".xml"));
        // get all of the non-hidden files
        File[] authorDirs = BekahUtil.listNotHiddenFiles(source);
        ArrayList<File> soICanShuffle = new ArrayList<>();
        Collections.addAll(soICanShuffle, authorDirs);
        Collections.shuffle(soICanShuffle);
        int i = 0;
        
        for(File authorDir : soICanShuffle)
        {
            TrainingAuthor t = new TrainingAuthor(authorDir.getName());
            File[] docs = BekahUtil.listNotHiddenFiles(authorDir);
            if(docs.length < min_docs)
            {
                continue;
            }
            for(File doc : docs)
            {
                t.addDoc(doc);
            }
            ps.addTrain(t);
            i++;
            if(i % numAuthors == 0)
            {
                ps.save();
                String path = saveLoc.getPath().replace(".xml", "_" + (i / numAuthors) + ".xml");
                ps = new GenericProblemSetCreator(path);
            }
        }
    }

    public void addTest(TrainingAuthor testFile)
    {
        testFiles.add(testFile);
    }

    public void addTrain(TrainingAuthor author)
    {
        trainFiles.add(author);
    }

    public void addTrain(File train)
    {
        TrainingAuthor a = new TrainingAuthor(train.getName());
        File[] docs = BekahUtil.listNotHiddenFiles(train);
        for(File f : docs)
        {
            a.addDoc(f);
        }
        this.addTrain(a);
    }

    public boolean save() throws IOException
    {
        //System.out.println("Saving to " + fileDest);
        if(trainFiles.isEmpty() && testFiles.isEmpty())
        {
            return false;
        }
        File f = new File(fileDest);
        File dir = f.getParentFile();
        if(!dir.exists())
        {
            dir.mkdirs();
        }
        if(!f.exists())
        {
            f.createNewFile();
        }

        FileWriter fstream = new FileWriter(f);

        BufferedWriter out = new BufferedWriter(fstream);

        String header = "<?xml version=\"1.0\"?>\n" +
                "<problem-set>\n" +
                "	<training name=\"bt\">\n";

        out.write(header);

        for(TrainingAuthor author : trainFiles)
        {
            out.write("\t\t<author name=\"" + author.getAuthorName() + "\">\n");
            ArrayList<File> docs = author.getDocs();
            for(File doc : docs)
            {
                out.write("\t\t\t<document title=");
                out.write("\"" + doc.getName() + "\">");
                out.write(doc.getPath());
                out.write("</document>\n");
            }
            out.write("\t\t</author>\n");
        }

        out.write("\t</training>\n");

        out.write("\t<test>");
        for(TrainingAuthor author : testFiles)
        {
            out.write("\t\t<author name=\"" + author.getAuthorName() + "\">\n");
            ArrayList<File> docs = author.getDocs();
            for(File doc : docs)
            {
                out.write("\t\t\t<document title=");
                out.write("\"" + doc.getName() + "\">");
                out.write(doc.getPath());
                out.write("</document>\n");
            }
            out.write("\t\t</author>\n");
        }

        out.write("\t\n</test>\n");
        out.write("</problem-set>");

        out.close();

        return true;
    }

    public static void main(String[] args) throws Exception
    {
        for(int i = 50; i < 1000; i += 50)
        {
            System.out.println(i);
            String dest = "/Applications/jsan-0.0.1/jsan_resources/bekahs_problem_sets/";
            GenericProblemSetCreator g = new GenericProblemSetCreator(
                    dest + "OnlyB_docssize_" + i + "numDocs_" + 10 + ".xml");
            File blogDir = new File("/Users/bekahoverdorf/Documents/PSAL/bt/" +
                    "blogs/grouped_blogs_" + i + "/");

            //int numGreaterThanTen = 0;
            if(!blogDir.exists())
            {
                throw new Exception(blogDir.getPath());
            }
            File[] authorDirs = BekahUtil.listNotHiddenFiles(blogDir);
            for(File author : authorDirs)
            {
                File[] docs = BekahUtil.listNotHiddenFiles(author);
//                if(docs.length > 10)
//                {
//                    numGreaterThanTen++;
//                }
                String name = author.getName();
                String authorNamePure = "";
                for(int j = 0; j < name.length(); j++)
                {
                    char c = name.charAt(j);
                    if(Character.isLetter(c))
                    {
                        authorNamePure += c;
                    }
                }
                TrainingAuthor a = new TrainingAuthor(authorNamePure);
                ArrayList<File> docList = new ArrayList<File>();
                Collections.addAll(docList, docs);
                a.setDocs(docList);
                g.addTrain(a);
            }
            g.save();
        }
    }

    @Override
    public String toString()
    {
        return fileDest;
    }

    int getNumTrainingAuthors()
    {
        return trainFiles.size();
    }

    public ProblemSet getProblemSet()
    {
        return new ProblemSet(fileDest);
    }

    static class TrainingAuthor
    {
        private String authorName;
        private ArrayList<File> docs;

        public TrainingAuthor(String authorName)
        {
            this.authorName = authorName;
            docs = new ArrayList<>();
        }

        public void addAllDocsAt(File loc)
        {
            File[] files = BekahUtil.listNotHiddenFiles(loc);
            Collections.addAll(docs, files);
        }

        public void setDocs(ArrayList<File> docs)
        {
            this.docs = docs;
        }

        public void addDoc(File doc)
        {
            docs.add(doc);
        }

        public String getAuthorName()
        {
            return authorName;
        }

        public ArrayList<File> getDocs()
        {
            return docs;
        }

        @Override
        public String toString()
        {
            String toPrint = authorName + " | " + "[";
            toPrint += docs.get(0).getName();
            for(int i = 1; i < docs.size(); i++)
            {
                File f = docs.get(i);
                toPrint += (", " + f.getName());
            }
            return toPrint + "]";
        }
    }
}
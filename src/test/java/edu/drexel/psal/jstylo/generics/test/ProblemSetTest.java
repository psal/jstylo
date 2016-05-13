package edu.drexel.psal.jstylo.generics.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.jgaap.generics.Document;

import edu.drexel.psal.jstylo.featureProcessing.ProblemSet;

public class ProblemSetTest {

    @Test
    public void testBuildProblemSets(){
        ProblemSet ps = new ProblemSet("./jsan_resources/problem_sets/drexel_1_train_test.xml");
        ProblemSet ps2 = new ProblemSet(ps);
        ProblemSet ps3 = new ProblemSet();
        
        for (Document doc : ps.getAllTrainDocs())
            ps3.addTrainDoc(doc.getAuthor(), doc);
        for (Document doc : ps.getAllTestDocs())
            ps3.addTestDoc(doc.getAuthor(), doc);
        
        String pstestpath = "./src/test/resources/pstest.xml";
        
        
        try {
            ps3.writeToXML(pstestpath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        File f = new File(pstestpath);
        f.delete();
        ps2.removeAuthor(ps2.getDummyAuthor());
        
        ProblemSet ps4 = new ProblemSet("./jsan_resources/problem_sets/drexel_1_train_test.xml",true);
        ProblemSet ps5 = new ProblemSet("./jsan_resources/problem_sets/drexel_1_train_test.xml",false);
    }
    
}

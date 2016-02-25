package edu.drexel.psal.jstylo.test;



import com.jgaap.generics.Document;
import edu.drexel.psal.jstylo.generics.ProblemSet;
import java.io.*;
import java.text.*;
import java.util.*;
import sun.audio.*;    //import the sun.audio package

/**
 *
 * @author bekahoverdorf
 */
public class BekahUtil
{
    /**
     * Empties Trash Directory (pretty quickly)
     */
    public static void emptyTrash()
    {
        System.out.print("> Empyting trash");
        deleteChildren(new File("/Users/bekahoverdorf/.Trash"));
        System.out.println(" complete");
    }

    /**
     * deletes children of given directory (if any).
     *
     * @param dontDeleteMe Directory who's children will be deleted.
     */
    public static void deleteChildren(File dontDeleteMe)
    {
        File[] toDelete = dontDeleteMe.listFiles();
        for (File f : toDelete)
        {
            deleteDirAndAllChildrenInner(f);
        }
    }

    public static void deleteDirAndAllChildren(File file)
    {
        Scanner in = new Scanner(System.in);
        System.out.println("Are you sure you want to delete: " +
                file.getAbsolutePath() + " and all of its children? (y): ");
        if (in.next().equals("y"))
        {
            System.out.println("deleation in progress... ");
            deleteDirAndAllChildrenInner(file);
        }
    }

    /**
     * deletes given directory (or file) and children (if any).
     *
     * @param dir the directory to be deleted.
     */
    private static void deleteDirAndAllChildrenInner(File file)
    {
        if (file.isDirectory())
        {
            //directory is empty, then delete it
            if (file.list().length == 0)
            {
                file.delete();
            }
            else
            {
                String files[] = file.list();
                for (String temp : files)
                {
                    File fileDelete = new File(file, temp);
                    deleteDirAndAllChildrenInner(fileDelete);
                }
                if (file.list().length == 0)
                {
                    file.delete();
                }
            }
        }
        else
        {
            file.delete();
        }
    }

    /**
     * Dumps all of the text in a file into a string. Returns empty string if
     * file is empty.
     *
     * @param file the file with text in it
     * @return
     * @throws FileNotFoundException if source is not found
     * @throws NoSuchElementException if the file is empty
     * @throws IllegalStateException if this scanner is closed
     */
    public static String getAllTextFrom(File file) throws FileNotFoundException
    {
        Scanner in = new Scanner(file);
        String next = "";
        try
        {
            next = in.useDelimiter("\\Z").next();
        }
        catch (NoSuchElementException ex)
        {
            in.close();
        }
        in.close();
        return next;
    }

    public static String removeQuotes(File f) throws FileNotFoundException
    {
        String newString = "";

        Scanner in1 = new Scanner(f);
        String allTextFromF = in1.useDelimiter("\\Z").next();

        allTextFromF = allTextFromF.replace("\n", " <br> ");

        Scanner in = new Scanner(allTextFromF);

        boolean record = true;
        while (in.hasNext())
        {
            String s = in.next();
            if (s.startsWith("\""))
            {
                record = false;
            }
            if (record)
            {
                newString += s + " ";
            }
            if (s.endsWith("\""))
            {
                record = true;
            }
        }
        newString = newString.replace("<br> ", "\n");

        in1.close();
        in.close();
        return newString;
    }

    /**
     * Returns the date and time in the form : M:dd:h:mm.
     *
     * @return
     */
    public static String getNow()
    {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("M:dd:h:mm");
        String now = ft.format(dNow);
        return now;
    }

    /**
     * Returns the date and time in the form : h:mm:ss.
     *
     * @return
     */
    public static String getTimeNow()
    {
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("h:mm:ss");
        String now = ft.format(dNow);
        return now;
    }

    /**
     * Use me to print out how far along you are in your for loop.
     *
     * How to use me:
     *
     * 1. before your loop make a string - initialize it to ""
     *
     * 2. Use that string as the third parameter
     *
     * 3. Set the return value of this method to that string
     *
     * 4. This will print out your progress every time the tenth decimal place
     * changes.
     *
     * @param i
     * @param total
     * @param printedPercentDone
     * @return
     */
    public static String printProgress(int i, int total,
            String printedPercentDone,
            int decimalPlaces)
    {

        double percentDone = (i / (double) total) * 100;
        String stringPercentDone = "" + percentDone;
        try
        {
            if (decimalPlaces == 0)
            {
                stringPercentDone = "" + (int) (Double.parseDouble(
                        stringPercentDone));
            }
            else
            {
                stringPercentDone = stringPercentDone.substring(0,
                        stringPercentDone.indexOf(".") + (decimalPlaces + 1));
            }
        }
        catch (Exception ex)
        {
            return printedPercentDone;
        }
        if (!printedPercentDone.equals(stringPercentDone))
        {
            System.out.println(stringPercentDone + "% complete");
            return stringPercentDone;
        }
        else
        {
            return printedPercentDone;
        }

    }

    public static File[] listNotHiddenFiles(File dir)
    {
        File[] list = dir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return !name.startsWith(".");
            }
        });
        return list;
    }

    /**
     * Saves the text given to the provided file. Creates file and directory if
     * it does not exist.
     *
     * @param f
     * @param textToWrite
     * @throws IOException
     */
    public static void saveMe(File f, String textToWrite) throws IOException
    {
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        FileWriter fstream = new FileWriter(f);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(textToWrite);
        out.close();
    }

    /**
     * Saves the text given to the provided file. Creates file and directory if
     * it does not exist.
     *
     * @param f
     * @param toWrite
     * @throws IOException
     */
    public static void saveMeBackup(File f, String toWrite) throws IOException
    {
        String backup = "/Users/bekahoverdorf/btbackup/timestamp" + BekahUtil.getNow();

        String path = f.getAbsolutePath();
        Scanner pathScanner = new Scanner(path).useDelimiter("/");

        boolean record = false;
        while (pathScanner.hasNext())
        {
            String temp = pathScanner.next();
            if (record)
            {
                backup += "/" + temp;
            }
            if (temp.equals("Documents") || temp.equals("NetBeansProjects"))
            {
                record = true;
            }
        }
        // make the backup


        File backupFile = new File(backup);


        File backupDir = new File(backupFile.getParent());
        backupDir.mkdirs();
        backupFile.createNewFile();

        FileWriter backupfstream = new FileWriter(backupFile);
        BufferedWriter backupout = new BufferedWriter(backupfstream);
        backupout.write(toWrite);
        backupout.close();

        // make the file
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        FileWriter fstream = new FileWriter(f);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(toWrite);
        out.close();
    }

    /**
     * counts the number of words in the given text file.
     *
     * @param f
     */
    public static int wordCounter(File f) throws FileNotFoundException
    {
        String text = BekahUtil.getAllTextFrom(f);
        if (text.contains("---"))
        {
            text = text.substring(text.indexOf("---"));
        }
        String[] split = text.split(" ");
        return split.length;

    }

    private static void findMe(String toFind, File inDir)
    {
        try
        {
            if (inDir.isDirectory())
            {

                //directory is empty, then stop looking
                if (inDir.listFiles().length == 0)
                {
                    // do nothing
                }
                else
                {
                    // Go through the children in the directory
                    String files[] = inDir.list();
                    for (String temp : files)
                    {
                        File childDir = new File(inDir, temp);
                        findMe(toFind, childDir);
                    }
                    if (inDir.list().length == 0)
                    {
                        if (inDir.getName().contains(toFind))
                        {
                            System.out.println(inDir.getAbsolutePath());
                        }
                    }
                }
            }
            else // inDir is a file
            {
                if (inDir.getName().contains(toFind))
                {
                    System.out.println(inDir.getAbsolutePath());
                }
            }
        }
        catch (Exception ex)
        {
        }
    }

    private static void countWords() throws InterruptedException
    {
        while (true)
        {
            try
            {
                int i = BekahUtil.wordCounter(new File(
                        "/Users/bekahoverdorf/Dropbox/MyDocuments/drexel/courses/" +
                        "AdvancesInSoftwareDesign/assignments/aspectJ.txt"));
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t" + (i));
                int thritySeconds = 1000 * 60 / 2;
                Thread.sleep(thritySeconds);
            }
            catch (Exception ex)
            {
                int oneMinute = 1000 * 60;
                int thritySeconds = 1000 * 60 / 2;
                Thread.sleep(thritySeconds);
            }
        }
    }

    public static String formatDate(String time, String day, String month, String year)
    {
        return day + month + year + " " + time;
    }

    public static void removeEmptyDocs(ProblemSet problemSet)
            throws FileNotFoundException
    {
        List<Document> allTrainDocs = problemSet.getAllTrainDocs();
        for (Document d : allTrainDocs)
        {
            File doc = new File(d.getFilePath());
            Scanner in = new Scanner(doc);
            if (!in.hasNext())
            {
                problemSet.removeTrainDocAt(d.getAuthor(), d.getTitle());
            }
            in.close();
        }
    }

    public static void filterunicode(File rawDir, File newDir) throws IOException
    {
        File[] authorDirs = BekahUtil.listNotHiddenFiles(rawDir);
        for (File a : authorDirs)
        {
            File[] docs = BekahUtil.listNotHiddenFiles(a);
            for (File doc : docs)
            {
                String filtered = "";
                char[] allText = BekahUtil.getAllTextFrom(doc).toCharArray();
                for (char c : allText)
                {
                    if ((int) c < 127)
                    {
                        filtered += c;
                    }
                    else {
                    	int ee = 0;
                    	ee = 1;
                    	if (ee == 1)
                    		ee = 2;
                    }
                }
                if (!filtered.equals(""))
                {
                    BekahUtil.saveMe(new File(newDir, a.getName() + "/" + doc.getName()), filtered);
                }
            }
        }
    }

    public static synchronized void playSound(File file) throws FileNotFoundException, IOException, InterruptedException
    {
        InputStream in = new FileInputStream(file);

        AudioStream as = new AudioStream(in);

        AudioPlayer.player.start(as);
        Thread.sleep(5000);
        AudioPlayer.player.stop(as);
    }

    public static synchronized void playSound() throws FileNotFoundException, IOException, InterruptedException
    {
        try
        {
            playSound(new File("/Users/bekahoverdorf/Desktop/Desktop/beep-7.wav"));
        }
        catch (Exception ex)
        {
            System.err.println("BEEP");
        }
    }

    public static String[] groupTweets(File[] tweets, int size) throws FileNotFoundException
    {
        System.out.println("Tweets: " + tweets.length);

        String[] ret = new String[(int) (Math.ceil(tweets.length / size))];
        int soFar = 0;

        String toSave = "";

        int i = 0;

        while (i < tweets.length - size)
        {
            while (soFar < size && i < tweets.length)
            {
                // tweets are "filtered_tweet.txt"
                File[] listNotHiddenFiles = listNotHiddenFiles(tweets[i]);
                for (File f : listNotHiddenFiles)
                {
                    if (f.getName().equals("filtered_tweet.txt"))
                    {
                        toSave += getAllTextFrom(f);
                        soFar++;
                    }
                }
                i++;
            }
            ret[i / size - 1] = toSave;
            toSave = "";
            soFar = 0;
        }
        return ret;
    }
}
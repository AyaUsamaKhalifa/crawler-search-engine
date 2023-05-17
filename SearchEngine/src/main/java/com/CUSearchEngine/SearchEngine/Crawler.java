package com.CUSearchEngine.SearchEngine;

import com.mongodb.client.*;
import com.mongodb.ConnectionString;

//import org.bson.Document;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler implements Runnable {
    //* Static data members:
    private static Set<Thread> threads = new HashSet<>();
    private static final int SHINGLE_SIZE = 5;
    private static final double JACCARD_SIMILARITY_THRESHOLD = 0.85;
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static MongoCollection<org.bson.Document> HTMLDocument;
    private static FileWriter CrawledURLsFile;
    private static HashMap<String, Set<String>> RefURLHashMap;
    private static FileWriter RefURLsFile;
    private static HashMap<String, Set<Integer>> VisitedURLsHash;
    private static Set<String> VisitedURLsSet;  //!NEW
    private static Set<String> CompactStringSet;    //!NEW
    private static HashMap<String, String> normalizedHM;
    private static Set<String> CrawledURLsSet;
    private static Queue<String> URLsToCrawlQ;  //!NEW
    private static FileWriter URLsToCrawlFile;
    private static FileWriter VisitedURLsFile;
    private static Integer NumOfThreads;
    private static Boolean LimitReached;
    private static Integer NumOfPages = 0;
    private static boolean Done = false;
    //* Non-static data members
    private String URL;
    private Crawler(String URL)
    {
        this.URL = URL;
    }

    @SneakyThrows
    @Override
    public void run() {

        crawl();
        // after all hyperlinks checked:
        //check if 6000 pages reached
        if(!LimitReached)
        {
            // less than 6000 pages
            // check if there is empty
            if(Crawler.is_empty_URLsToCrawlPQ())
            {
                //URLList is empty
                synchronized (Crawler.NumOfThreads)
                {
                    Crawler.NumOfThreads--;
                }
                System.out.println("URL list is empty: terminate");
            }
            else
            {
                String NewURL = Crawler.get_url_URLsTOCrawlPQ();
                this.URL = NewURL;
                crawl();
            }
        }
        else
        {
            //6000 reached
            //add URLs in CrawledURLs file in htmlDocument collection
            Crawler.TerminateCrawler();
            System.out.println("Limit is reached: terminate");
        }
    }
    private void crawl() throws IOException, URISyntaxException {
        if (LimitReached) {
            return;
        }
        System.out.println("Start Crawling");
        System.out.println(this.URL);
        Document doc = null;
        boolean Found = true;
        try {
            doc = Jsoup.connect(URL).get();
        } catch (IOException e) {
            System.out.println("error " + e.toString());
            Found = false;
        }
        if(Found)
        {
            Elements links = doc.select("a");
            // print out the hyperlinks
            for (Element link : links)
            {
                if (LimitReached) {
                    return;
                }
                String HyperLink = link.attr("href");
                if (!HyperLink.startsWith("http") && !HyperLink.startsWith("https")) {
                    HyperLink = URL + HyperLink;
                }
                boolean FoundHL = true;
                Connection ConnForRobot = null;
                try {
                    Connection connection = Jsoup.connect(HyperLink);
                    ConnForRobot = connection.ignoreContentType(true);
                    doc = connection.get();
//                    doc = Jsoup.connect(HyperLink).get();
                } catch (IOException e) {
                    System.out.println("error " + e.toString());
                    FoundHL = false;
                }
                if(!FoundHL)
                {
                    continue;
                }
                //Normalize URL
                URL docUrl = new URL(doc.location());
                URL normalizedUrl = docUrl.toURI().normalize().toURL();
                if(!check_visited_spam(doc, normalizedUrl.toString()))
                {
                    //True = element added (not found)
                    //False = element not added (found)
                    Crawler.add_in_RefHashMap(doc ,normalizedUrl.toString(), this.URL);
                    continue;
                }
                if(!check_robot_txt(HyperLink, ConnForRobot))
                {
                    continue;
                }
                if(Crawler.add_in_RefHashMap(doc ,normalizedUrl.toString(), this.URL))
                {
                    //Not visited before
                    //add it to the URLsTOCrawlPG
                    Crawler.add_url_in_URLsTOCrawlQ(HyperLink, normalizedUrl.toString());
                    Crawler.start_new_crawler();
                }
                else
                {
                    //visited
                    continue;
                }
            }
        }
        //Start a new take a new url for the same crawler
        if(!LimitReached)
        {
            if(!Crawler.is_empty_URLsToCrawlPQ())
            {
                String NewURL = Crawler.get_url_URLsTOCrawlPQ();
                this.URL = NewURL;
                crawl();
            }
        }
    }
    private static void start_new_crawler() throws IOException {
        synchronized (Crawler.NumOfThreads)
        {
            if(Crawler.NumOfThreads > 0)
            {
                String NewURL = get_url_URLsTOCrawlPQ();
                if(NewURL != null)
                {
                    //start new crawler;
                    Crawler.NumOfThreads --;
                    Crawler NewCrawler = new Crawler(NewURL);
                    Thread NewThread = new Thread(NewCrawler);
                    synchronized (Crawler.threads){
                        threads.add(NewThread);
                    }
                    NewThread.start();
                    System.out.println("crawler " + Crawler.NumOfThreads + " started");
                }
            }
        }
    }

    //* ______________________________________ Updating Data Structures ______________________________________
    //*____________________________ URLsToCrawl ________________________________
    private static boolean add_in_RefHashMap(Document doc, String normalizedUrl, String href) throws URISyntaxException, MalformedURLException {
        //Normalize URL
        Set<String> RefURLs = new HashSet<>();
        RefURLs.add(href);
        synchronized (Crawler.RefURLHashMap)
        {
            Set<String> RefIn = Crawler.RefURLHashMap.putIfAbsent(normalizedUrl, RefURLs);
            if(RefIn == null)
            {
                //Was not there before
                return true;
            }
            else
            {
                //was there before
                // so add new href to the url
                RefIn.add(href);
                Crawler.RefURLHashMap.put(normalizedUrl, RefIn);
                return false;
            }
        }
    }
    private static void add_url_in_URLsTOCrawlQ(String NewURL, String normalizedUrl) throws IOException {
        synchronized (Crawler.URLsToCrawlQ)
        {
            Crawler.URLsToCrawlQ.add(NewURL);
        }
        synchronized (Crawler.normalizedHM)
        {
            Crawler.normalizedHM.put(NewURL, normalizedUrl);
        }
        synchronized (Crawler.NumOfPages)
        {
            Crawler.NumOfPages ++;
            System.out.println("Increment pages = " + Crawler.NumOfPages + "  " + NewURL);
            //check if 6000 pages is reached
            if(Crawler.NumOfPages >= 500)
            {
                // Limit is reached
                synchronized (Crawler.LimitReached)
                {
                    Crawler.LimitReached = true;
                }
            }
        }
    }
    private static String get_url_URLsTOCrawlPQ() throws IOException {
        synchronized (Crawler.URLsToCrawlQ)
        {
            if(!Crawler.URLsToCrawlQ.isEmpty())
            {
                String TempURL = Crawler.URLsToCrawlQ.remove();
                add_url_in_CrawledURLs(TempURL);
                return TempURL;
            }
            return null;
        }
    }
    private static boolean is_empty_URLsToCrawlPQ()
    {
        synchronized (Crawler.URLsToCrawlQ)
        {
            return Crawler.URLsToCrawlQ.isEmpty();
        }
    }
    private static synchronized void TerminateCrawler() throws IOException, URISyntaxException {
        if(Crawler.Done)
        {
            System.out.println("Already Terminated");
            return;
        }
        Crawler.Done = true;
        System.out.println("__________________ Start Terminating ___________________________");
        int Count = 0;
        List<org.bson.Document> ListDoc = new ArrayList<>();
        String URLToAdd = "";
        synchronized (Crawler.CrawledURLsSet) {
            Iterator<String> iterator = Crawler.CrawledURLsSet.iterator();
            while (iterator.hasNext() && Count!=500) {
                URLToAdd = iterator.next();
                String normalizedUrl = Crawler.normalizedHM.get(URLToAdd);
                // find its Ref in
                Set<String> href = Crawler.RefURLHashMap.get(normalizedUrl);
                if (href == null) {
                    System.out.println("SSEEEEEEEDDDD");
                    org.bson.Document document = new org.bson.Document();
                    document.append("URL", URLToAdd);
                    document.append("RefIn", new ArrayList<>());
                    ListDoc.add(document);
                    Count++;
                    continue;
                }

                System.out.println(href);
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                document.append("RefIn", href);
                ListDoc.add(document);
                Count++;
            }
        }
        synchronized (Crawler.URLsToCrawlQ) {
            while (!Crawler.URLsToCrawlQ.isEmpty()  && Count!=500) {
                URLToAdd = Crawler.URLsToCrawlQ.remove();
                String normalizedUrl = Crawler.normalizedHM.get(URLToAdd);
                // find its Ref in
                Set<String> href = Crawler.RefURLHashMap.get(normalizedUrl);
                if (href == null) {
                    System.out.println("SSEEEEEEEDDDD");
                    org.bson.Document document = new org.bson.Document();
                    document.append("URL", URLToAdd);
                    document.append("RefIn", new ArrayList<>());
                    ListDoc.add(document);
                    Count++;
                    continue;
                }

                System.out.println(href);
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                document.append("RefIn", href);
                ListDoc.add(document);
                Count++;
            }
        }
        //Set database collections
        Crawler.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
        Crawler.db = mongoClient.getDatabase("SearchEngine-api-db");
        Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler");
        Crawler.HTMLDocument.insertMany(ListDoc);
        System.out.println("__________________ Start Terminating ___________________________");
    }

    private static synchronized void add_url_in_CrawledURLs(String NewURL) throws IOException
    {
        synchronized (Crawler.CrawledURLsSet)
        {
            Crawler.CrawledURLsSet.add(NewURL);
        }
    }

    // _______________________Populate data structures ______________________
//TODO: Change
    //Function that populates URLList by URLsToCrawlFile
    private static void populate_URLsToCrawlPQ()
    {
        try {
            FileReader fr = new FileReader("URLsToCrawl.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                Crawler.URLsToCrawlQ.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Function that populates VisitedURLsHash with VisitedURL file
    private static void populate_VisitedURLsHash()
    {
        try {
            FileReader fr = new FileReader("VisitedURLs.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                // get the urls and the associated compact string
                String[] parts = line.split("=");
                String key = parts[0];
                String value = parts[1];
                // get the shingle set
//                Set<Integer> ShingleSet = getHashedShingles(value);
                // add in VisitedURLsHash
//                Crawler.VisitedURLsHash.put(key, ShingleSet);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Function that populated RefURLHashMap with RefURLFile
    private static void populate_RefURLHashMap()
    {
        try {
            FileReader fr = new FileReader("RefURLs.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=");
                String key = parts[0];
                String[] values = parts[1].split(",");
                Set<String> set = new HashSet<>(Arrays.asList(values));
                Crawler.RefURLHashMap.put(key, set);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //* __________________________________ check URL methods _______________________________
    //! ________________________________ NEW _________________________
    private static boolean check_visited_spam(Document doc, String normalizedUrl) throws IOException, URISyntaxException {
        //Create compact string
        HtmlCompressor compressor = new HtmlCompressor();
        String compactHtml = compressor.compress(doc.text());
        compactHtml.replaceAll("[^a-zA-Z0-9]+", " ").toLowerCase();
        //check if visited before:
        synchronized (Crawler.VisitedURLsSet)
        {
            if(Crawler.VisitedURLsSet.add(normalizedUrl.toString()))
            {
                //True = element added (not found)
                synchronized (Crawler.CompactStringSet)
                {
                    return Crawler.CompactStringSet.add(compactHtml);
                }
            }
            else
            {
                //False = element not added (found)
                return false;
            }
        }
    }
    //! ________________________________ NEW _________________________
    private static boolean check_robot_txt(String NewURL, Connection connection) {
        boolean Found = true;
        String path = "";
        Document doc = null;
        try {
            URL urlObj = new URL(NewURL);
            String protocol = urlObj.getProtocol();
            String hostname = urlObj.getHost();
            path = urlObj.getPath();
            String rootUrl = protocol + "://" + hostname + "/robots.txt";
            doc = Jsoup.connect(rootUrl).get();
        } catch (IOException e) {
            System.out.println("error " + e.toString());
            Found = false;
        }
        if(!Found)
        {
            System.out.println("robots.txt not found");
            return true;
        }

        String robotsTxt = doc.text();
        String DisallowString = "Disallow: ";
        String UserAgentString = "User-agent: ";
        int UserAgentIndex = robotsTxt.indexOf(UserAgentString);
        if(UserAgentIndex != -1)
        {
            if(robotsTxt.charAt(UserAgentIndex + UserAgentString.length() + 1) == '*')
            {
                //check the disallowed
                int IndexFound = 0;
                while((IndexFound = robotsTxt.indexOf(DisallowString, IndexFound)) != -1)
                {
                    IndexFound = IndexFound + DisallowString.length();
                    int EndIndex = robotsTxt.indexOf(' ', IndexFound);
                    if(EndIndex == -1)      //it is the last disallow
                    {
                        EndIndex = robotsTxt.length();
                    }
                    String directory = robotsTxt.substring(IndexFound, EndIndex).trim();
                    // Create a Pattern object from the regex string
                    if(directory.indexOf('*') != -1)
                    {
                        directory = directory.replace("*", ".*");
                        Pattern pattern = Pattern.compile(directory);
                        Matcher matcher = pattern.matcher(path);
                        if (matcher.find())
                        {
                            System.out.println(NewURL + " disallowed");
                            return false;
                        }
                    }
                    if(directory.endsWith("$"))
                    {
                        //check directory is at the end of the path
                        if(path.endsWith(directory))
                        {
                            System.out.println(NewURL + " disallowed");
                            return false;
                        }
                    }
                    else
                    {
                        if(path.startsWith(directory))
                        {
                            System.out.println(NewURL + " disallowed");
                            return false;
                        }
                    }
                }
                System.out.println(NewURL + " allowed");
                return true;
            }
            else
            {
                System.out.println(NewURL + " allowed");
                return true;
            }
        }
        System.out.println(NewURL + " allowed");
        return true;
    }
    //* _______________________________ Main ___________________________________
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException
    {
        // Initializing static variables
        // take number of threads from user as input and set NumOfThreads
        //! Uncomment
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Enter maximum number of threads: ");
//        Crawler.NumOfThreads = scanner.nextInt() -1;
//        scanner.close();
        //! Comment
        //!NEW
        Crawler.CompactStringSet = new HashSet<>();
        Crawler.VisitedURLsSet = new HashSet<>();
        Crawler.CrawledURLsSet = new HashSet<>();

        Crawler.NumOfThreads = 10;
        //Set files
        Crawler.URLsToCrawlQ = new LinkedList<String>();
        Crawler.LimitReached = false;
        //Initialize data structures
        Crawler.RefURLHashMap = new HashMap<>();
        Crawler.VisitedURLsHash = new HashMap<>();
        Crawler.normalizedHM = new HashMap<>();

        int NumOfCrawledURLs = 0;
        int NumOfVisitedURLs = 0;
        if(NumOfCrawledURLs == 0)
        {
            //TODO: and empty db or do it manually
            //Seed set
            Set<String> SeedSet = new HashSet<>();
            SeedSet.add("https://www.imdb.com/");
            SeedSet.add("https://edition.cnn.com/");
            SeedSet.add("https://www.pinterest.com/");
            SeedSet.add("https://stackoverflow.com/");

            for (String Seed: SeedSet)
            {
                Document doc = null;
                boolean Found = true;
                try {
                    doc = Jsoup.connect(Seed).get();
                } catch (IOException e) {
                    System.out.println("error " + e.toString());
                    Found = false;
                }
                if(!Found) continue;
                URL docUrl = new URL(doc.location());
                URL normalizedUrl = docUrl.toURI().normalize().toURL();
                Crawler.add_url_in_URLsTOCrawlQ(Seed, normalizedUrl.toString());
            }
        }
        else if(NumOfVisitedURLs == 0)
        {
            //fill data structures
            Crawler.populate_URLsToCrawlPQ();
            Crawler.populate_RefURLHashMap();
            //continue populating db with Crawled URLs
            Crawler.TerminateCrawler();
        }
        else
        {
            // populates data structures
            Crawler.populate_VisitedURLsHash();
            Crawler.populate_URLsToCrawlPQ();
            Crawler.populate_RefURLHashMap();
            //initialize number of pages
            Crawler.NumOfPages = NumOfCrawledURLs;
        }

        // Start initial thread
        start_new_crawler();
    }
}

/*
add another collection for crawler list, and for the compact strings
Data members
! Static:
    * number of threads
    * list of URLS, compact strings,...

* Start with Initial list --> either just seed or previous list of URLs
?--------> threads and synchronization ?
    * we will have max number of threads taken from the user as input
    * so, after each thread finished crawling a url, it checks if the max thread number is reached,
    * if not reached, create a new thread
    * and for the same thread, it crawls another url in the list


* For a thread: (it has 1 url)
1) get document of URL
2) get hyperlinks in webpage--> for each hyperlink
? robot.txt
* get robot .txt
* check what should be avoided to crawl and don't add them in the list
    * 1. check if it is previously visited  --> set of visited urls
    * 2. check if it is similar to another one using compact strings and normalized URLs:
        for 1 and 2
        Data structure needed --> list of pairs (url, compact string)
        * loop on urls compact string and the urls themselves
            * first check the normalized url (if they are the same)
            ? check if the compact string is similar --> check algorithms
            * for the compact string -->  use library

3) if the checks passed:
    * add the URL in the list and in db:
        ? data structure
            * priority queue --> where the most referenced website is crawled first
            * for every refin added to a url + 1, or just get the length of array of refin
    * add its compact string in the list and db
4) remove url from list and db
    * check if 6000 pages reached
        * if reached, add what is in URLList to the html documents db
        ? check first that they are not visited or spam --> or just ignore it
        * delete crawler urls and crawled urls db


! Databases:
* database to save crawler urls with ref in
* database to contain crawled urls and their compact string

*/

/*
! how to check for spam and similarity:
    * add all urls visited in hash table
    * first check if normalized(current url) found in hash table
        * if yes, continue
        * if no, iterate on hash table to check for spam
            * whether equal or almost similar

*/

/*
! what needs synchronization:
    * accessing list of urls to crawl URLList
    * accessing visited urls hash map
    * accessing database (either read or write)
    * accessing number of threads

*/

//! utitlities:
//* long numDocuments = collection.countDocuments();    //to get the number of documents in a collection4
//* hashtable.put("apple", 1);                          //add
//* int value = hashtable.get("banana");                //retrieve

/*
String key = "Shingles";
    String[] ShingleList = {"value1", "value2", "value3"};

    Document document = new Document("URL", "https//fsdfsdf.com")
            .append(key, Arrays.asList(valuesToAdd));

      collection.insertOne(document);
*/


/*
 for (String key : hashTable.keySet()) {
    Integer value = hashTable.get(key);
}
* */

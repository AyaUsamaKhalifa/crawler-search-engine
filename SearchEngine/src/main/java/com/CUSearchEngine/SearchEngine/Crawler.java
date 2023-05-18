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


import java.io.IOException;
import java.net.URL;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;


public class Crawler implements Runnable {
    //* Static data members:
    private static final int TOTALNUMPAGES = 6000;
    private static Set<Thread> threads = new HashSet<>();
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static MongoCollection<org.bson.Document> HTMLDocument;
    private static HashMap<String, Set<String>> RefURLHashMap;
    private static int Count = 0;
    private static Set<String> VisitedURLsSet;  //!NEW
    private static Set<String> CompactStringSet;    //!NEW
    private static HashMap<String, String> normalizedHM;
    private static Set<String> CrawledURLsSet;
    private static Queue<String> URLsToCrawlQ;  //!NEW
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
                try {
                    doc = Jsoup.connect(HyperLink).get();
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
                if(!check_robot_txt(HyperLink))
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
                    Crawler.start_new_crawler();
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
            if(Crawler.NumOfPages >= TOTALNUMPAGES)
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
        List<org.bson.Document> ListDoc = new ArrayList<>();
        String URLToAdd = "";
        synchronized (Crawler.CrawledURLsSet) {
            Iterator<String> iterator = Crawler.CrawledURLsSet.iterator();
            while (iterator.hasNext() && Crawler.Count!=TOTALNUMPAGES) {
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
                    Crawler.Count++;
                    continue;
                }

                System.out.println(href);
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                document.append("RefIn", href);
                ListDoc.add(document);
                Crawler.Count++;
            }
        }
        synchronized (Crawler.URLsToCrawlQ) {
            while (!Crawler.URLsToCrawlQ.isEmpty()  && Crawler.Count!=TOTALNUMPAGES) {
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
                    Crawler.Count++;
                    continue;
                }

                System.out.println(href);
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                document.append("RefIn", href);
                ListDoc.add(document);
                Crawler.Count++;
            }
        }
        if(ListDoc.size() != 0)
        {
            //Set database collections
            Crawler.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7"));
            Crawler.db = mongoClient.getDatabase("SearchEngine-api-db");
//            Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler");
            Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler2");
            Crawler.HTMLDocument.insertMany(ListDoc);
            Crawler.mongoClient.close();
        }

        MongoClient CrawlerClient = MongoClients.create("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7");
        MongoDatabase Crawlerdb = CrawlerClient.getDatabase("crawler-intermediate-data");
        MongoCollection<org.bson.Document> VisitedURLsC = Crawlerdb.getCollection("VisitedURLs");
        VisitedURLsC.deleteMany(new org.bson.Document());
        MongoCollection<org.bson.Document> RefInURLsC = Crawlerdb.getCollection("RefInURLs");
        RefInURLsC.deleteMany(new org.bson.Document());
        MongoCollection<org.bson.Document> NormalizedURLsC = Crawlerdb.getCollection("NormalizedURLs");
        NormalizedURLsC.deleteMany(new org.bson.Document());
        MongoCollection<org.bson.Document> URLsToCrawlC = Crawlerdb.getCollection("URLsToCrawl");
        URLsToCrawlC.deleteMany(new org.bson.Document());
        MongoCollection<org.bson.Document> VisitedCompactStringsC = Crawlerdb.getCollection("VisitedCompactStrings");
        VisitedCompactStringsC.deleteMany(new org.bson.Document());
        System.out.println("__________________ END Terminating ___________________________");
    }

    private static synchronized void add_url_in_CrawledURLs(String NewURL) throws IOException
    {
        synchronized (Crawler.CrawledURLsSet)
        {
            Crawler.CrawledURLsSet.add(NewURL);
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
    private static boolean check_robot_txt(String NewURL) throws MalformedURLException {
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
//            if(robotsTxt.charAt(UserAgentIndex + UserAgentString.length() + 1) == '*')
            String userAgents = robotsTxt.substring(UserAgentIndex + UserAgentString.length() + 1);
            if (userAgents.contains("*"))
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
                    String directory = "";
                    if (IndexFound >= 0 && EndIndex >= IndexFound && EndIndex <= robotsTxt.length())
                    {
                        directory = robotsTxt.substring(IndexFound, EndIndex).trim();
                    }
                    else
                    {
                        continue;
                    }

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
    //* ______________________ Exiting ___________________
    private static void exit_crawler() throws IOException, URISyntaxException {
        //if the limit is already reached
        if(Crawler.LimitReached)
        {
            Crawler.Done = false;
            Crawler.TerminateCrawler();
        }
        else
        {
            // Save what is in the crawled URLs to HTMLDocuments
            List<org.bson.Document> ListDoc = new ArrayList<>();
            Iterator<String> iterator = Crawler.CrawledURLsSet.iterator();
            String URLToAdd = "";
            while (iterator.hasNext()) {
                URLToAdd = iterator.next();
                String normalizedUrl = Crawler.normalizedHM.get(URLToAdd);
                // find its Ref in
                Set<String> href = Crawler.RefURLHashMap.get(normalizedUrl);
                if (href == null) {
                    org.bson.Document document = new org.bson.Document();
                    document.append("URL", URLToAdd);
                    document.append("RefIn", new ArrayList<>());
                    ListDoc.add(document);
                    continue;
                }
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                document.append("RefIn", href);
                ListDoc.add(document);
            }
            //Add it to database
            Crawler.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7"));
            Crawler.db = mongoClient.getDatabase("SearchEngine-api-db");
//            Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler");
            Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler2");
            Crawler.HTMLDocument.insertMany(ListDoc);
            Crawler.mongoClient.close();
            //*__________________________________________________________________________________________
            // Save the visited URLs set
            List<org.bson.Document> ListDoc1 = new ArrayList<>();
            Iterator<String> iterator1 = Crawler.VisitedURLsSet.iterator();
            URLToAdd = "";
            while (iterator1.hasNext()) {
                URLToAdd = iterator1.next();
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                ListDoc1.add(document);
            }
            //Add it to database
            MongoClient CrawlerClient = MongoClients.create("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7");
            MongoDatabase Crawlerdb = CrawlerClient.getDatabase("crawler-intermediate-data");
            MongoCollection<org.bson.Document> VisitedURLsC = Crawlerdb.getCollection("VisitedURLs");
            VisitedURLsC.insertMany(ListDoc1);
            //*__________________________________________________________________________________________
            // Save the compact string set
            List<org.bson.Document> ListDoc2 = new ArrayList<>();
            Iterator<String> iterator2 = Crawler.CompactStringSet.iterator();
            String CompString = "";
            while (iterator2.hasNext()) {
                CompString = iterator2.next();
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("CompactString", CompString);
                ListDoc2.add(document);
            }
            //Add it to database
            MongoCollection<org.bson.Document> VisitedCompactStringsC = Crawlerdb.getCollection("VisitedCompactStrings");
            VisitedCompactStringsC.insertMany(ListDoc2);
            //*__________________________________________________________________________________________
            //save the URLs to crawl
            List<org.bson.Document> ListDoc5 = new ArrayList<>();
            while (!Crawler.URLsToCrawlQ.isEmpty()) {
                URLToAdd = Crawler.URLsToCrawlQ.remove();
                //add it in db
                org.bson.Document document = new org.bson.Document();
                document.append("URL", URLToAdd);
                ListDoc5.add(document);
            }
            MongoCollection<org.bson.Document> URLsToCrawlC = Crawlerdb.getCollection("URLsToCrawl");
            URLsToCrawlC.insertMany(ListDoc5);
            //*__________________________________________________________________________________________
            // Save the Normalized URLs map
            List<org.bson.Document> ListDoc3 = new ArrayList<>();
            for (Map.Entry<String, String> entry : Crawler.normalizedHM.entrySet()) {
                org.bson.Document document = new org.bson.Document();
                document.append("URL", entry.getKey());
                document.append("NormalizedURL", entry.getValue());
                ListDoc3.add(document);
            }
            //Add it to database
            MongoCollection<org.bson.Document> NormalizedURLsC = Crawlerdb.getCollection("NormalizedURLs");
            NormalizedURLsC.insertMany(ListDoc3);
            //*__________________________________________________________________________________________
            // save RefIn map
            List<org.bson.Document> ListDoc4 = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : Crawler.RefURLHashMap.entrySet()) {
                org.bson.Document document = new org.bson.Document();
                document.append("URL", entry.getKey());
                document.append("RefIn", entry.getValue());
                ListDoc4.add(document);
            }
            //Add it to database
            MongoCollection<org.bson.Document> RefInURLsC = Crawlerdb.getCollection("RefInURLs");
            RefInURLsC.insertMany(ListDoc4);

            // Save number of pages
            org.bson.Document document = new org.bson.Document();
            document.append("Pages", Crawler.NumOfPages);
            URLsToCrawlC.insertOne(document);

            CrawlerClient.close();
        }
    }

    private static void populate_data_structures(MongoDatabase Crawlerdb, MongoCollection<org.bson.Document> URLsToCrawlC)
    {
        System.out.println("Populating data structures");
        // Crawlerdb
        // Find all documents in the collection
        FindIterable<org.bson.Document> docs = URLsToCrawlC.find();
        // Iterate over the documents and add each URL to the queue
        for (org.bson.Document doc : docs) {
            String url = doc.getString("URL");
            if (url != null && !url.isEmpty()) {
                Crawler.URLsToCrawlQ.add(url);
            }
        }
        URLsToCrawlC.deleteMany(new org.bson.Document());
        //* ____________________________________________________________
        MongoCollection<org.bson.Document> RefInURLsC = Crawlerdb.getCollection("RefInURLs");
        // Find all documents in the collection
        docs = RefInURLsC.find();
        // Iterate over the documents and populate the map
        for (org.bson.Document doc : docs) {
            String url = doc.getString("URL");
            Set<String> refUrls = new HashSet<>();
            // Get the 'RefIn' field as an array and add each value to the set
            List<String> refUrlsList = (List<String>) doc.get("RefIn");
            if (refUrlsList != null) {
                for (String refUrl : refUrlsList) {
                    if (refUrl != null && !refUrl.isEmpty()) {
                        refUrls.add(refUrl);
                    }
                }
            }

            // Add the URL and set of ref URLs to the map
            if (url != null && !url.isEmpty()) {
                Crawler.RefURLHashMap.put(url, refUrls);
            }
        }
        RefInURLsC.deleteMany(new org.bson.Document());
        //* ____________________________________________________________
        MongoCollection<org.bson.Document> NormalizedURLsC = Crawlerdb.getCollection("NormalizedURLs");
        // Find all documents in the collection
        docs = NormalizedURLsC.find();
        // Iterate over the documents and populate the map
        for (org.bson.Document doc : docs) {
            String url = doc.getString("URL");
            String normalizedUrl = doc.getString("NormalizedURL");

            // Add the URL and normalized URL to the map
            if (url != null && !url.isEmpty() && normalizedUrl != null && !normalizedUrl.isEmpty()) {
                Crawler.normalizedHM.put(url, normalizedUrl);
            }
        }
        NormalizedURLsC.deleteMany(new org.bson.Document());
        //* ____________________________________________________________
        MongoCollection<org.bson.Document> VisitedCompactStringsC = Crawlerdb.getCollection("VisitedCompactStrings");
        // Find all documents in the collection
        docs = VisitedCompactStringsC.find();
        // Iterate over the documents and populate the set
        for (org.bson.Document doc : docs) {
            String compString = doc.getString("CompactString");
            // Add the compact string to the set
            if (compString != null && !compString.isEmpty()) {
                Crawler.CompactStringSet.add(compString);
            }
        }
        VisitedCompactStringsC.deleteMany(new org.bson.Document());
        //* ____________________________________________________________
        MongoCollection<org.bson.Document> VisitedURLsC = Crawlerdb.getCollection("VisitedURLs");
        docs = VisitedURLsC.find();
        // Iterate over the documents and populate the set
        for (org.bson.Document doc : docs) {
            String url = doc.getString("URL");
            // Add the URL to the set
            if (url != null && !url.isEmpty()) {
                Crawler.VisitedURLsSet.add(url);
            }
        }
        VisitedURLsC.deleteMany(new org.bson.Document());
        //* ____________________________________________________________
        //populates crawled urls and delete HTMLDocument
        Crawler.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7"));
        Crawler.db = mongoClient.getDatabase("SearchEngine-api-db");
//        Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler");
        Crawler.HTMLDocument = Crawler.db.getCollection("TempDbCrawler2");
        docs = Crawler.HTMLDocument.find();
        // Iterate over the documents and add each URL to the queue
        for (org.bson.Document doc : docs) {
            String url = doc.getString("URL");
            if (url != null && !url.isEmpty()) {
                Crawler.CrawledURLsSet.add(url);
            }
        }
        Crawler.HTMLDocument.deleteMany(new org.bson.Document());
        Crawler.mongoClient.close();
    }
    private static boolean check_if_interrupted()
    {
        //see if the Pages document is found in the URLsToCrawl collection
        MongoClient CrawlerClient = MongoClients.create("mongodb+srv://samiha:Owxx9hDm5NFC4ANa@cluster0.4nczz1o.mongodb.net/7");
        MongoDatabase Crawlerdb = CrawlerClient.getDatabase("crawler-intermediate-data");
        MongoCollection<org.bson.Document> URLsToCrawlC = Crawlerdb.getCollection("URLsToCrawl");
        org.bson.Document doc = URLsToCrawlC.find(new org.bson.Document("Pages", new org.bson.Document("$exists", true))).limit(1).first();
        if (doc == null) {
            // document not found
            CrawlerClient.close();
            return false;
        } else {
            // document found
            //set the number of pages
            Crawler.NumOfPages = doc.getInteger("Pages");
            if(Crawler.NumOfPages >= Crawler.TOTALNUMPAGES)
            {
                Crawler.LimitReached = true;
            }
            URLsToCrawlC.deleteOne(doc);
            //populate the data structures
            Crawler.populate_data_structures(Crawlerdb, URLsToCrawlC);
            CrawlerClient.close();
            return true;
        }
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
        Crawler.NumOfThreads = 10;

        //!NEW
        Crawler.CompactStringSet = new HashSet<>();
        Crawler.VisitedURLsSet = new HashSet<>();
        Crawler.CrawledURLsSet = new HashSet<>();


        Crawler.URLsToCrawlQ = new LinkedList<String>();
        Crawler.LimitReached = false;
        //Initialize data structures
        Crawler.RefURLHashMap = new HashMap<>();
        Crawler.normalizedHM = new HashMap<>();

        //check whether the crawler was interrupted before
        if(!check_if_interrupted())
        {
            //Seed set
            Set<String> SeedSet = new HashSet<>();

            SeedSet.add("https://www.imdb.com/");                       //*1
            SeedSet.add("https://www.amazon.com/");                     //*2
            SeedSet.add("https://www.pinterest.com/");                  //*3
            SeedSet.add("https://education.nationalgeographic.org/");   //*4
            SeedSet.add("https://www.nationalgeographic.org/");         //*5
            SeedSet.add("https://www.goodreads.com/");                  //*6
            SeedSet.add("https://www.jstor.org/");                      //*7
            SeedSet.add("https://www.newscientist.com/");               //*8
            SeedSet.add("https://www.politico.com/");                   //*9
            SeedSet.add("https://www.encyclopedia.com/");               //*10
            SeedSet.add("https://www.reuters.com/");                    //*11
            SeedSet.add("https://www.unesco.org/");                     //*12
            SeedSet.add("https://www.wikihow.com/");                    //*13
            SeedSet.add("https://www.espn.in/");                        //*14
            SeedSet.add("https://en.wikipedia.org/");                   //*15
            SeedSet.add("https://www.sciencenews.org/");                //*16
            SeedSet.add("https://www.who.int/");                        //*17
            SeedSet.add("https://www.woah.org/");                       //*18
            SeedSet.add("https://olympics.com/");                       //*19
            SeedSet.add("https://www.allrecipes.com/");                 //*20




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
        // Start initial thread
        start_new_crawler();

        //! ________________________________ NEW ____________________________________________
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Exiting...");
            for(Thread t: Crawler.threads){
                t.interrupt();
            }
            // Perform cleanup tasks or save state information here
            try {
                Crawler.exit_crawler();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }));
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

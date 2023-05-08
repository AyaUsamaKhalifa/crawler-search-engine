package com.CUSearchEngine.SearchEngine;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.ConnectionString;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
//import org.bson.Document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.jsoup.nodes.Element;

import java.net.URISyntaxException;
import java.net.URL;

import java.io.IOException;
import java.text.Normalizer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Crawler implements Runnable {
    //* Static data members:
    private static final int SHINGLE_SIZE = 40;
    private static final double JACCARD_SIMILARITY_THRESHOLD = 0.9;
    public static MongoClient mongoClient;
    public static MongoDatabase db;
    public static Integer NumOfThreads;
    public static List<String> URLList;
    public static HashMap<String, Set<String>> VisitedURLsHash;
    private static Boolean LimitReached = false;
    private static Integer NumOfPages = 0;
    //* Non-static data members
    private String URL;
    public Crawler(String URL)
    {
        this.URL = URL;
    }

    @Override
    public void run() {
        crawl();
        // after all hyperlinks checked:
        //check if 6000 pages reached
        if(!LimitReached)
        {
            // less than 6000 pages
            // check if there is empty
            System.out.println("less than 6000");
            if(Crawler.is_empty_URLList())
            {
                //URLList is empty
                //TODO: terminate thread
                synchronized (Crawler.NumOfThreads)
                {
                    Crawler.NumOfThreads--;
                }
                System.out.println("URL list is empty: terminate");
            }
            else
            {
                System.out.println("URL is not empty");
                // create new thread if possible and take another value from the URLList
                String NewURL = Crawler.get_url_URLList();
                //check if max number of threads is reached
                if(Crawler.start_new_crawler(NewURL))
                {
                    System.out.println("New thread created");
                    //new thread is created, so take next URL if possible
                    if(Crawler.is_empty_URLList())
                    {
                        //URLList is empty
                        //TODO: terminate thread
                        synchronized (Crawler.NumOfThreads)
                        {
                            Crawler.NumOfThreads--;
                        }
                        System.out.println("terminate 1");
                    }
                    else
                    {
                        NewURL = Crawler.get_url_URLList();
                    }
                }
                this.URL = NewURL;
                crawl();
            }
        }
        else
        {
            //6000 reached
            //add URLs in URLList in htmlDocument collection
            Crawler.TerminateCrawler();
            //TODO: terminate :
                //? remove documents in db
            System.out.println("terminate");
        }

    }


    //* ______________________________________ Accessing URLList ______________________________________
    //TODO get back to old add and get url list thing
    private synchronized static String get_url_URLList()
    {
        synchronized (Crawler.URLList)
        {
            String TempURL = Crawler.URLList.get(0);
            Crawler.URLList.remove(TempURL);
            add_url_in_HTMLDocuments(TempURL);
            return TempURL;
        }
    }
    private static boolean add_url_in_URLList(String NewURL)
    {
        synchronized (Crawler.URLList)
        {
            Crawler.URLList.add(NewURL);
            synchronized(Crawler.NumOfPages)
            {
                Crawler.NumOfPages ++;
                System.out.println("Increment pages = " + Crawler.NumOfPages + "  " + NewURL);
                //check if 6000 pages is reached
                if(Crawler.NumOfPages < 5999)
                {
                    return true;
                }
                else
                {
                    // Limit is reached
                    synchronized (Crawler.LimitReached)
                    {
                        Crawler.LimitReached = true;
                    }
                    return false;
                }
            }
        }
    }
    private static boolean is_empty_URLList()
    {
        synchronized (Crawler.URLList)
        {
            return Crawler.URLList.isEmpty();
        }
    }
    private static void TerminateCrawler()
    {
        while(!Crawler.is_empty_URLList())
        {
            //Read data from the URLList
                String NewURL = Crawler.get_url_URLList();
        }
    }
    private static boolean start_new_crawler(String NewURL)
    {
        synchronized (Crawler.NumOfThreads)
        {
            if(Objects.equals(NewURL, ""))
            {
                return Crawler.NumOfThreads > 0;
            }
            if(Crawler.NumOfThreads > 0)
            {
                //start new crawler;
                Crawler.NumOfThreads --;
                Crawler NewCrawler = new Crawler(NewURL);
                Thread NewThread = new Thread(NewCrawler);
                NewThread.start();
                System.out.println("crawler " + Crawler.NumOfThreads + " started");
                return true;
            }
        }
        return false;
    }
    //* ___________________________________ Access HTMLDocument ___________________________________
    private synchronized static void add_url_in_HTMLDocuments(String newURL)
    {
        synchronized (Crawler.db)
        {
            MongoCollection<org.bson.Document> collection = Crawler.db.getCollection("TempDbCrawler");
            org.bson.Document document = new org.bson.Document("URL", newURL);
            synchronized (collection)
            {
                collection.insertOne(document);
                System.out.println("Database Updated");
            }
        }
    }
    private void crawl()
    {
        System.out.println("Start Crawling");
        System.out.println(this.URL);
        Document doc = null;
        try {
            doc = Jsoup.connect(URL).get();
        } catch (IOException e) {
            System.out.println("error " + e.toString());

        }
        // find all links in the HTML document
        Elements links = doc.select("a");
        // print out the hyperlinks
        for (Element link : links) {
            String href = link.attr("href");
            if(!href.startsWith("http") && !href.startsWith("https"))
            {
                href = URL + href;
            }
            //new URL are ready
            // TODO: check Robot.txt
            // check if visited before or spam
            try {
                if(check_visited_spam(href))
                {
                    // visited before or spam
                    // the above function will add the url with its compact string in the hash table \
                    // for it to be recognized easily if it appeared again
                    continue;
                }
                else
                {
                    //not visited
                    //add it to the URLList
                    if(LimitReached)
                    {
                        break;
                    }
                    if(!Crawler.add_url_in_URLList(href))
                    {
                        //* Limit is reached
                        // if limit is reached, we won't add more URLs in the URLList,
                        // but we will add the urls already in the URLList in the db (HTMLDocuments)
                        // since, we guarantee that those URLs are unique
                        break;
                    }
                    else
                    {
                        //* Limit is not reached
                        // create a thread if possible
                        if(Crawler.start_new_crawler(""))
                        {
                            // thread can be created
                            String NewURL = Crawler.get_url_URLList();
                            Crawler.start_new_crawler(NewURL);
                        }
                    }
                }
            } catch (IOException | URISyntaxException e) {
                System.out.println("error " + e.toString());
            }
        }
    }
    //* __________________________________ check URL methods _______________________________
    private static Set<String> getShingles(String text) {
        // Break up the text into shingles of the specified size
        Set<String> shingles = new HashSet<>();
        for (int i = 0; i <= text.length() - Crawler.SHINGLE_SIZE; i++) {
            String substring = text.substring(i, i + Crawler.SHINGLE_SIZE);
            shingles.add(substring);
        }
        return shingles;
    }
    private static boolean  check_visited_spam(String NewURL) throws IOException, URISyntaxException {
        //Normalize URL
        Document doc = null;
        try {
            doc = Jsoup.connect(NewURL).get();
        } catch (IOException e) {
            System.out.println("error " + e.toString());
            return true;
        }
        URL docUrl = new URL(doc.location());
        URL normalizedUrl = docUrl.toURI().normalize().toURL();
        //check if visited before:
        synchronized (Crawler.VisitedURLsHash)
        {
            if (Crawler.VisitedURLsHash.containsKey(normalizedUrl.toString())) {
                // Visited before:
                return true;
            }

        // not visited before
        //* Prepare data to compare:
        //Create compact string
        HtmlCompressor compressor = new HtmlCompressor();
        String compactHtml = compressor.compress(doc.text());
        compactHtml.replaceAll("[^a-zA-Z0-9]+", " ").toLowerCase();
        // Break up the cleaned HTML content of each web page into shingles
        Set<String> NewshingleSet = Crawler.getShingles(compactHtml);
        //check for spam for each url in the hash table

            for (String key : Crawler.VisitedURLsHash.keySet()) {
                Set<String> OldShingleSet = Crawler.VisitedURLsHash.get(key);
                if(is_spam(NewshingleSet, OldShingleSet))
                {
                    //spam
                    //add URL in hash table
                    Crawler.VisitedURLsHash.put(normalizedUrl.toString(), NewshingleSet);
                    return true;
                }
            }
            // not spam
            //add URL in hash table
            Crawler.VisitedURLsHash.put(normalizedUrl.toString(), NewshingleSet);
            return false;
        }
    }

    private static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        // Calculate the Jaccard similarity between the two sets
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        double jaccard = (double) intersection.size() / (double) union.size();
        return jaccard;
    }
    private static boolean is_spam(Set<String> ShingleSet1, Set<String> ShingleSet2) {
        double similarity = Crawler.calculateJaccardSimilarity(ShingleSet1, ShingleSet2);
        // Determine if the web pages are near-duplicates
        return similarity >= Crawler.JACCARD_SIMILARITY_THRESHOLD;
    }

    public  void JsoupExample() throws IOException, NoSuchAlgorithmException {
        //* -------------- to normalize URL --------------
//        String url = "https://tinyurl.com/app";
//        Document doc = Jsoup.connect(url).get();
//        URL normalizedUrl = new URL(doc.location());
//        System.out.println("Normalized URL: " + normalizedUrl.toString());
//
        //* -------------- to get html file --------------
//        Document html = null;
//        String url = "https://www.google.com/search?client=firefox-b-d&q=football";
//        try {
//            html = Jsoup.connect(String.valueOf(url)).get();
//            // Use the doc object to traverse and manipulate the HTML content
////            System.out.println(html);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //* -------------- to remove tags --------------
//        Document doc = Jsoup.parse(String.valueOf(html));
//        String text = doc.text();

        //* -------------- to check similarity between strings --------------
        Document html1 = null;
        Document html2 = null;
        try {
            html1 = Jsoup.connect(String.valueOf("https://jsoup.org/apidocs/")).get();
            html2 = Jsoup.connect(String.valueOf("https://jsoup.org/")).get();
            // Use the doc object to traverse and manipulate the HTML content
//            System.out.println(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String text1 = Jsoup.parse(String.valueOf(html1)).text();
//        String text2 = Jsoup.parse(String.valueOf(html2)).text();
//
//        // Remove unwanted characters and normalize whitespace
//        text1 = text1.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().replaceAll("\\s+", " ").trim();
//        text2 = text2.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().replaceAll("\\s+", " ").trim();
//
//        // Compute the Levenshtein distance between the text content
//        LevenshteinDistance distance = new LevenshteinDistance();
//        double distanceValue = (double) distance.apply(text1, text2);
//
//        // Compute the similarity between the text content
//        double similarity = 1 - distanceValue / Math.max(text1.length(), text2.length());
//
//        double similarityThreshold = 0.5;
////        LevenshteinDistance distance = new LevenshteinDistance();
////        double similarity = 1 - (double) distance.apply(str1, str2) / Math.max(str1.length(), str2.length());
//        System.out.println(similarity);
//        if (similarity >= similarityThreshold) {
//            System.out.println("The strings are almost equal.");
//        } else {
//            System.out.println("The strings are not almost equal.");
//        }
//        Document doc = Jsoup.parse(html);
//***************************************************************
        // Create a new HtmlCompressor object
        HtmlCompressor compressor = new HtmlCompressor();

        // Compress the HTML document
        assert html1 != null;
        String compactHtml = compressor.compress(html1.text());

        // Print the compact HTML string
        System.out.println(compactHtml);
        System.out.println(compactHtml.length());
        System.out.println(html1.toString().length());

    }
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        // Initializing static variables
        // TODO: take number of threads from user as input
        Crawler.NumOfThreads = 10;
        Crawler.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
        Crawler.db = mongoClient.getDatabase("SearchEngine-api-db");
        Crawler.VisitedURLsHash = new HashMap<>();
        Crawler.URLList = new ArrayList<>();
        String url = "https://en.wikipedia.org/wiki/Wikipedia:WikiProject_Lists_of_topics";
        Crawler.URLList.add(url);
        Crawler.check_visited_spam(url);
        // add URL in db
        Crawler.add_url_in_HTMLDocuments(url);
        //TODO: get the list of urls from db and check if empty, add seed. if not, add it to the list
        String StartURL =  Crawler.URLList.get(0);
        Crawler.URLList.remove(StartURL);
        Crawler crawler = new Crawler(StartURL);
        Thread InitialThread = new Thread(crawler);
        InitialThread.start();
    }
}

/*
TODO: add another collection for crawler list, and for the compact strings
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
 for (String key : hashTable.keySet()) {
    Integer value = hashTable.get(key);
}
* */

package com.CUSearchEngine.SearchEngine;

import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tartarus.snowball.ext.porterStemmer;

import static java.lang.Math.log;


public class Indexer {
    public double calc_IDF(long num_docs, long num_docs_word) {
        return (log((double) num_docs / num_docs_word)) / log(2);
    }

    //a hashset of words which are going to be ignored when parsing the html documents
    //O(1) for looking up a word
    HashSet<String> stopWords = new HashSet<>(Arrays.asList("a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
            "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
            "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
            "other", "ought", "our", "ours	ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
            "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's",
            "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"));


    //class that holds the data of each word
    static class Data {
        String URL;
        String paragraph;
        double TF = 0;
        String title;
        int noOccBold;
        int H1;
        int H2;
        int H3;
        int H4;
        int H5;
        int H6;
        int noOccTitle;
        String filePath;
        HashSet<String> originalWords;
        int totalOcc;

        Data() {
            paragraph = "";
            noOccTitle = 0;
            originalWords = new HashSet<>();
            totalOcc = 0;
            noOccBold = 0;
            H1 = 0;
            H2 = 0;
            H3 = 0;
            H4 = 0;
            H5 = 0;
            H6 = 0;
        }
    }

    //word class where we will store the information of each word
    //this class corresponds to the document that will be stored in the db, which represents the inverted file
    class Word {
        private String word;
        private HashMap<String, Data> data;

        Word(String word) {
            this.word = word;
            this.data = new HashMap<>();
        }

        boolean addWebsite(String url,String filePath) {
            if (!data.containsKey(url)) {
                Data newData = new Data();
                newData.URL = url;
                newData.filePath = filePath;
                data.put(url, newData);
                return false;
            }
            return true;
        }


    }

    Word addWord(String currentWord, porterStemmer stemmer, HashMap<String, Word> wordsMap, String url,String filePath) {
        //stemming the word
        stemmer.setCurrent(currentWord);
        stemmer.stem();
        currentWord = stemmer.getCurrent();
        //if the word is not included in the words map, create a new word
        if (!wordsMap.containsKey(currentWord)) {
            Word newWord = new Word(currentWord);
            //insert the word object in the words hashmap
            wordsMap.put(currentWord, newWord);
            //add website to the word's data map
            newWord.addWebsite(url,filePath);
            return newWord;
        } else {
            Word existingWord = wordsMap.get(currentWord);
            //add website to the word's data map
            existingWord.addWebsite(url,filePath);
            return existingWord;

        }
    }

    void runIndexer() throws IOException {
        //stemmer object
        porterStemmer stemmer = new porterStemmer();
        //connecting to the database
        MongoClient mongoClient;
        MongoDatabase db;
        mongoClient = MongoClients.create(new ConnectionString("mongodb://localhost:27017/"));
        db = mongoClient.getDatabase("LocalDB");

        //getting the InvertedFiles collection
        MongoCollection<Document> IFcollection = db.getCollection("InvertedFiles");

        //retrieving the list of indexed urls if there is any in the db
        HashSet<String> indexedURLS = IFcollection.distinct("indexedurls", String.class).into(new HashSet<>());
        //hashset of the new set of the urls to be indexed in this run, will be appended in the database
        HashSet<String> newIndexedURLS = new HashSet<>();

        //getting the HTMLDocuments collection
        MongoCollection<Document> collection = db.getCollection("HTMLDocuments");

        //the map that will hold the words and information about them, which will later be written
        //into the inverted files documents in the database
        HashMap<String, Word> wordsMap = new HashMap<>();

        //number of documents in the collection
        long totalNumDocs = collection.countDocuments();

        //hashset to represent the spam sites, will be removed after the for loop
        Set<String> spam = new HashSet<>();

        //calculate popularity of the urls in the DB
        // Retrieve a value of a key inside a document of documents
        MongoCursor<Document> result = collection.find().cursor();
        Map<String, ArrayList<String>> resultMap = new HashMap<>();
        while (result.hasNext()) {
            Document URLS = result.next();
            resultMap.put((String) URLS.get("URL"), (ArrayList<String>) URLS.get("RefIn"));
        }
        popularity popularity = new popularity();
        HashMap<String, Double> popularityMap = popularity.calculatePopularity(resultMap);
        int docnum = 1;
        String workingDir = System.getProperty("user.dir");

        //loop over the documents in the collection to get corresponding html docs
        for (Document doc : collection.find()) {
            String url = doc.getString("URL");
            String filePath = doc.getString("filePath");

            //add the url to the already indexed url set (index only once)
            if (indexedURLS.contains(url)) {
                continue;
            }

            org.jsoup.nodes.Document currentHTMLdoc;
            try {
                //getting the html of the url
                File input = new File(workingDir+"\\src\\main\\java\\com\\CUSearchEngine\\SearchEngine\\"+filePath);
                currentHTMLdoc = Jsoup.parse(input);
                System.out.println("new doc" + docnum);
                docnum++;
                newIndexedURLS.add(url);

            } catch (Exception e) {
                continue;
            }


            //getting all the html elements
            Elements elements = currentHTMLdoc.select("body, title, h1, h2, h3, h4, h5, h6, b");

            //getting the title text separately before the loop as the title is added to the data of all words that appear in the html document
            String titleText = currentHTMLdoc.select("title").text();

            //list to store the stemmed document words, to calculate their tf after the for loop
            List<String> totalDocWords = new ArrayList<>();

            //total number of words in the document (used to calculate tf)
            long totalDocWordCount = 0;

            //looping over the html elements and checking for specific elements from which we
            //want to collect data about the words for the ranker
            for (org.jsoup.nodes.Element element : elements) {
                String tagName = element.tagName();
                switch (tagName) {
                    case "title" -> {
                        //the structure of the html file is mainly the HEAD and the BODY
                        //we only care about the title in the head,
                        // get the title from the head and parse it
                        String[] words = titleText.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace
                        totalDocWordCount += words.length;
                        //parse the title
                        for (int j = 0; j < words.length; j++) {
                            String currentWord = words[j];
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //add stemmed word to the list of document words
                            totalDocWords.add(wordObj.word);
                            //increment the number of occurrences in the title
                            wordObj.data.get(url).noOccTitle++;
                            //increment the total occurrences
                            wordObj.data.get(url).totalOcc++;
                            //adding title
                            wordObj.data.get(url).title = titleText;
                            //adding the non-stemmed word
                            wordObj.data.get(url).originalWords.add(currentWord);
                        }
                    }
                    //////////////////////////////////////////////
                    case "body" -> {
                        //getting the text of the body and looping over it
                        //regardless of its position inside the tags
                        //String bodyText = currentHTMLdoc.select("body").text();
                        String bodyText = element.text();
                        String[] bodyWords = bodyText.split("[\\s\\p{Punct}]+"); //splitting on whitespace
                        totalDocWordCount += bodyWords.length;
                        //parse the body
                        for (int j = 0; j < bodyWords.length; j++) {
                            String originalWord = bodyWords[j];
                            //we do this here only in the body as we need the original word not the lower case version
                            //to extract its paragraph correctly
                            //as the use of .indexOf() requires the word to be the same as in the original text
                            String currentWord = originalWord.toLowerCase();
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //add stemmed word to the list of document words
                            totalDocWords.add(wordObj.word);
                            //increment the total occurrences
                            wordObj.data.get(url).totalOcc++;
                            //adding the non-stemmed word
                            wordObj.data.get(url).originalWords.add(currentWord);
                            //adding title
                            wordObj.data.get(url).title = titleText;
                            //adding the paragraph in which the word first occurred
                            if (wordObj.data.get(url).paragraph == "") {
                                //get a substring which includes the current word
                                int substringStartIndex = bodyText.indexOf(originalWord);
                                int substringEndIndex = bodyText.indexOf(originalWord) + currentWord.length();
                                int wordCount = 0;
                                //we are aiming to get 30 words before currentword and 30 words after it
                                while (substringStartIndex - 1 > 0) {
                                    //if a word was passed
                                    if (bodyText.charAt(substringStartIndex) == ' ') {
                                        wordCount++;
                                    }
                                    if (wordCount >= 30)
                                        break;
                                    substringStartIndex--;
                                }
                                //reset the word count
                                wordCount = 0;
                                while (substringEndIndex + 1 < bodyText.length()) {
                                    //if a word was passed
                                    if (bodyText.charAt(substringEndIndex) == ' ') {
                                        wordCount++;
                                    }
                                    if (wordCount >= 30)
                                        break;
                                    substringEndIndex++;
                                }
                                wordObj.data.get(url).paragraph = bodyText.substring(substringStartIndex, substringEndIndex);
                            }
                        }
                    }
                    //////////////////////////////////////////////
                    case "h1" -> {
                        String h1Text = element.text();
                        String[] h1TextWords = h1Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String h1TextWord : h1TextWords) {
                            String currentWord = h1TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h1 occurrences
                            wordObj.data.get(url).H1++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "h2" -> {
                        String h2Text = element.text();
                        String[] h2TextWords = h2Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String h2TextWord : h2TextWords) {
                            String currentWord = h2TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h2 occurrences
                            wordObj.data.get(url).H2++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "h3" -> {
                        String h3Text = element.text();
                        String[] h3TextWords = h3Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String h3TextWord : h3TextWords) {
                            String currentWord = h3TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h3 occurrences
                            wordObj.data.get(url).H3++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "h4" -> {
                        String h4Text = element.text();
                        String[] h4TextWords = h4Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String h4TextWord : h4TextWords) {
                            String currentWord = h4TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h4 occurrences
                            wordObj.data.get(url).H4++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "h5" -> {
                        String h5Text = element.text();
                        String[] h5TextWords = h5Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace & special characters

                        //parse the text
                        for (String h5TextWord : h5TextWords) {
                            String currentWord = h5TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h5 occurrences
                            wordObj.data.get(url).H5++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "h6" -> {
                        String h6Text = element.text();
                        String[] h6TextWords = h6Text.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String h6TextWord : h6TextWords) {
                            String currentWord = h6TextWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the h6 occurrences
                            wordObj.data.get(url).H6++;
                        }
                    }
                    //////////////////////////////////////////////
                    case "b" -> {
                        String boldText = element.text();
                        String[] boldWords = boldText.toLowerCase().split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the text
                        for (String boldWord : boldWords) {
                            String currentWord = boldWord;
                            //filtering non english words
                            if (currentWord.matches(".*[^\\p{ASCII}\\p{Punct}\\p{Digit}\\s].*")) {
                                continue;
                            }
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord, stemmer, wordsMap, url,filePath);
                            //increment the bold occurrences
                            wordObj.data.get(url).noOccBold++;
                        }
                    }
                    //////////////////////////////////////////////
                    default -> {
                    }
                }
            }
            //after looping on the document and collecting the data for each word we loop on the body a second time to calculate the TF
            for (String bodyWord : totalDocWords) {
                double TF = (double) wordsMap.get(bodyWord).data.get(url).totalOcc / totalDocWordCount;
                //checking if website is a spam
                if (TF >= 0.5) {
                    //add site to spam list
                    spam.add(url);
                } else {
                    wordsMap.get(bodyWord).data.get(url).TF = TF;

                }
            }
        }
        if (! indexedURLS.isEmpty()) { //update
            //write to the database
            //getting the InvertedFiles collection
            MongoCollection<Document> invertedFilesCollection = db.getCollection("InvertedFiles");
            List<UpdateOneModel<Document>> updates = new ArrayList<>();
            //list of all the documents to be inserted in the database using the insert many query
            List<Document> documentList = new ArrayList<Document>();
            //loop over the words in the map
            for (Map.Entry<String, Word> word : wordsMap.entrySet()) {
                ArrayList<Document> dataDocuments = new ArrayList<>();
                //loop over the urls in the word
                for (Map.Entry<String, Data> data : word.getValue().data.entrySet()) {
                    //if the website is spam, don't add it to the database for any word
                    if (spam.contains(data.getValue().URL)) {
                        continue;
                    }
                    Document dataDocument = new Document("URL", data.getValue().URL)
                            .append("paragraph", data.getValue().paragraph)
                            .append("TF", data.getValue().TF)
                            .append("title", data.getValue().title)
                            .append("noOccBold", data.getValue().noOccBold)
                            .append("H1", data.getValue().H1)
                            .append("H2", data.getValue().H2)
                            .append("H3", data.getValue().H3)
                            .append("H4", data.getValue().H4)
                            .append("H5", data.getValue().H5)
                            .append("H6", data.getValue().H6)
                            .append("noOccTitle", data.getValue().noOccTitle)
                            .append("originalWords", data.getValue().originalWords)
                            .append("filePath",data.getValue().filePath)
                            .append("totalOcc", data.getValue().totalOcc);
                    dataDocuments.add(dataDocument);
                }
                Document filter = new Document("word", word.getValue().word);
                Document update = new Document("$push", new Document("data", new Document("$each", dataDocuments)));
                UpdateOptions options = new UpdateOptions().upsert(true);
                updates.add(new UpdateOneModel<>(filter, update, options));
            }
            //updating the indexed urls
            Document filter = new Document("name", "indexed");
            Document update = new Document("$push", new Document("indexedurls", new Document("$each", newIndexedURLS)));
            invertedFilesCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
            if (!updates.isEmpty()) {
                invertedFilesCollection.bulkWrite(updates);

                // Define the constant num_docs value
                long num_docs = indexedURLS.size() + newIndexedURLS.size();

                // Create the aggregation pipeline to calculate the IDF value and update the existing IDF field
                List<Document> pipeline = Arrays.asList(
                        new Document("$addFields", new Document("IDF", new Document("$cond", Arrays.asList(
                                new Document("$and", Arrays.asList(
                                        new Document("$ifNull", Arrays.asList("$data", false)),
                                        new Document("$ne", Arrays.asList("$data", new BsonNull()))
                                )),
                                new Document("$divide", Arrays.asList(
                                        new Document("$ln", new Document("$divide", Arrays.asList(num_docs, new Document("$size", "$data")))),
                                        new Document("$ln", 2)
                                )),
                                0
                        ))))
                );

                // Execute the aggregation pipeline and update each document with the new IDF value
                //this has to be executed after the updates are written to the database because it needs the updates array size of the urls of each word
                invertedFilesCollection.updateMany(new Document(), pipeline);
            }
        } else { //insert
            //inserting in the DB
            //write to the database
            //getting the HTMLDocuments collection
            MongoCollection<Document> invertedFilesCollection = db.getCollection("InvertedFiles");
            //list of all the documents to be inserted in the database using the insert many query
            List<Document> documentList = new ArrayList<Document>();
            //loop over the words in the map
            for (Map.Entry<String, Word> word : wordsMap.entrySet()) {
                ArrayList<Document> dataDocuments = new ArrayList<>();
                //loop over the urls in the word
                for (Map.Entry<String, Data> data : word.getValue().data.entrySet()) {
                    //if the website is spam, don't add it to the database for any word
                    if (spam.contains(data.getValue().URL)) {
                        continue;
                    }
                    Document dataDocument = new Document("URL", data.getValue().URL)
                            .append("paragraph", data.getValue().paragraph)
                            .append("TF", data.getValue().TF)
                            .append("title", data.getValue().title)
                            .append("noOccBold", data.getValue().noOccBold)
                            .append("H1", data.getValue().H1)
                            .append("H2", data.getValue().H2)
                            .append("H3", data.getValue().H3)
                            .append("H4", data.getValue().H4)
                            .append("H5", data.getValue().H5)
                            .append("H6", data.getValue().H6)
                            .append("noOccTitle", data.getValue().noOccTitle)
                            .append("totalOcc", data.getValue().totalOcc)
                            .append("filePath",data.getValue().filePath)
                            .append("originalWords", data.getValue().originalWords);
                    dataDocuments.add(dataDocument);
                }
                Document wordDoc = new Document("word", word.getValue().word).append("data", dataDocuments);
                documentList.add(wordDoc);
            }

            invertedFilesCollection.insertMany(documentList);

            //Define the constant num_docs value
            long num_docs = indexedURLS.size() + newIndexedURLS.size();

            // Create the aggregation pipeline to calculate the IDF value and update the existing IDF field
            List<Document> pipeline = Arrays.asList(
                    new Document("$addFields", new Document("IDF", new Document("$cond", Arrays.asList(
                            new Document("$and", Arrays.asList(
                                    new Document("$ifNull", Arrays.asList("$data", false)),
                                    new Document("$ne", Arrays.asList("$data", new BsonNull()))
                            )),
                            new Document("$divide", Arrays.asList(
                                    new Document("$ln", new Document("$divide", Arrays.asList(num_docs, new Document("$size", "$data")))),
                                    new Document("$ln", 2)
                            )),
                            0
                    ))))
            );

            // Execute the aggregation pipeline and update each document with the new IDF value
            //this has to be executed after the updates are written to the database because it needs the updates array size of the urls of each word
            invertedFilesCollection.updateMany(new Document(), pipeline);
        }

        //updating the popularity
        MongoCollection<Document> popularityCollection = db.getCollection("URLPopularity2");

        // Create a list of update models
        List<UpdateOneModel<Document>> popularityUpdates = new ArrayList<>();

        // Loop over the entries in your map and create an update model for each one
        for (Map.Entry<String, Double> entry : popularityMap.entrySet()) {
            // Define the filter to identify the document to update based on the URL field
            Document popularityFilter = new Document("url", entry.getKey());

            // Define the update object to set the popularity field
            Document popularityUpdate = new Document("$set", new Document("popularity", entry.getValue()));

            // Create an update model and add it to the list of updates
            UpdateOneModel<Document> updateModel = new UpdateOneModel<>(popularityFilter, popularityUpdate, new UpdateOptions().upsert(true));
            popularityUpdates.add(updateModel);
        }

        // Execute the bulkWrite operation
        popularityCollection.bulkWrite(popularityUpdates);

        // Close the MongoDB client
        mongoClient.close();
    }

    public static void main(String[] args) throws IOException {
        Indexer myindexer = new Indexer();
        myindexer.runIndexer();
    }
}

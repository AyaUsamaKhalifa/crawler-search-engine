package com.CUSearchEngine.SearchEngine;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.*;
import org.jsoup.select.Elements;
import org.tartarus.snowball.ext.porterStemmer;



public class Indexer {
    //a hashset of words which are going to be ignored when parsing the html documents
    //O(1) for looking up a word
    HashSet<String> stopWords = new HashSet<>(Arrays.asList("a","about","above","after","again","against","all","am","an","and","any","are","aren't","as","at","be","because","been","before","being","below","between","both","but","by","can't","cannot","could","couldn't",
            "did","didn't","do","does","doesn't","doing","don't","down","during","each","few","for","from","further","had","hadn't","has","hasn't","have","haven't","having","he","he'd","he'll","he's","her","here","here's","hers","herself",
            "him","himself","his","how","how's","i","i'd","i'll","i'm","i've","if","in","into","is","isn't","it","it's","its","itself","let's","me","more","most","mustn't","my","myself","no","nor","not","of","off","on","once","only","or",
            "other","ought","our","ours	ourselves","out","over","own","same","shan't","she","she'd","she'll","she's","should","shouldn't","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there",
            "there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","wasn't","we","we'd","we'll","we're","we've","were","weren't","what","what's","when","when's",
            "where","where's","which","while","who","who's","whom","why","why's","with","won't","would","wouldn't","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"));



    //class that holds the data of each word
    static class Data {
        String URL;
        String paragraph;
        int TF=0;
        int IDF=0;
        String title;
        int noOccBold;
        int H1;
        int H2;
        int H3;
        int H4;
        int H5;
        int H6;
        int date=0;
        int noOccTitle;
        ArrayList<Integer> titlePosition;
        ArrayList<Integer> bodyPosition;
        int totalOcc;
        Data(){
            paragraph = "";
            noOccTitle = 0;
            titlePosition = new ArrayList<>();
            bodyPosition = new ArrayList<>();
            totalOcc = 0;
            noOccBold=0;
            H1=0;
            H2=0;
            H3=0;
            H4=0;
            H5=0;
            H6=0;
        }
    }
    //word class where we will store the information of each word
    //this class corresponds to the document that will be stored in the db, which represents the inverted file
    class Word {
        private String word;
        private HashMap<String, Data> data;

        Word (String word){
            this.word=word;
            this.data = new HashMap<>();
        }

        boolean addWebsite(String url){
            if(!data.containsKey(url)){
                Data newData= new Data();
                newData.URL=url;
                data.put(url,newData);
                return false;
            }
            return true;
        }


    }

    Word addWord(String currentWord,porterStemmer stemmer,HashMap<String,Word> wordsMap, String url){
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
            newWord.addWebsite(url);
            return newWord;
        } else {
            Word existingWord = wordsMap.get(currentWord);
            //add website to the word's data map
            existingWord.addWebsite(url);
            return existingWord;

        }
    }
    void runIndexer() throws IOException {
        //stemmer object
        porterStemmer stemmer = new porterStemmer();
        //connecting to the database
        MongoClient mongoClient;
        MongoDatabase db;
        mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
        db = mongoClient.getDatabase("SearchEngine-api-db");
        //getting the HTMLDocuments collection
        MongoCollection<Document> collection = db.getCollection("HTMLDocuments");

        //the map that will hold the words and information about them, which will later be written
        //into the inverted files documents in the database
        HashMap<String, Word> wordsMap = new HashMap<>();

        //loop over the documents in the collection to get corresponding html docs
        for (Document doc : collection.find()) {
            String url = doc.getString("URL");
            //getting the html of the url
            org.jsoup.nodes.Document currentHTMLdoc = Jsoup.connect(url).get();
            System.out.println("new doc");
            //getting all the html elements
            Elements elements = currentHTMLdoc.getAllElements();
            //getting the title text separately before the loop as the title is added to the data of all words that appear in the html document
            String titleText = currentHTMLdoc.select("title").text();
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

                        //parse the title
                        for (int j=0;j<words.length;j++) {
                            String currentWord = words[j];
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
                            //increment the number of occurrences in the title
                            wordObj.data.get(url).noOccTitle++;
                            //increment the total occurrences
                            wordObj.data.get(url).totalOcc++;
                            //adding title
                            wordObj.data.get(url).title=titleText;
                            //getting the position of the word in the title
                            wordObj.data.get(url).titlePosition.add(j);
                        }
                    }
                    //////////////////////////////////////////////
                    case "body" -> {
                        //getting the text of the body and looping over it
                        //regardless of its position inside the tags
                        //String bodyText = currentHTMLdoc.select("body").text();
                        String bodyText = element.text();
                        String[] bodyWords = bodyText.split("[\\s\\p{Punct}]+"); //splitting on whitespace

                        //parse the body
                        for (int j=0;j< bodyWords.length;j++) {
                            String originalWord=bodyWords[j];
                            //we do this here only in the body as we need the original word not the lower case version
                            //to extract its paragraph correctly
                            //as the use of .indexOf() requires the word to be the same as in the original text
                            String currentWord = originalWord.toLowerCase();
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
                            //increment the total occurrences
                            wordObj.data.get(url).totalOcc++;
                            //getting the position of the text
                            wordObj.data.get(url).bodyPosition.add(j);
                            //adding title
                            wordObj.data.get(url).title=titleText;
                            //adding the paragraph in which the word first occurred
                            if(wordObj.data.get(url).paragraph==""){
                                //get a substring which includes the current word
                                int substringStartIndex= bodyText.indexOf(originalWord);
                                int substringEndIndex= bodyText.indexOf(originalWord)+currentWord.length();
                                int wordCount=0;
                                //we are aiming to get 30 words before currentword and 30 words after it
                                while(substringStartIndex-1>0){
                                    //if a word was passed
                                    if(bodyText.charAt(substringStartIndex)==' '){
                                        wordCount++;
                                    }
                                    if(wordCount >= 30)
                                        break;
                                    substringStartIndex--;
                                }
                                //reset the word count
                                wordCount = 0;
                                while(substringEndIndex+1<bodyText.length()){
                                    //if a word was passed
                                    if(bodyText.charAt(substringEndIndex)==' '){
                                        wordCount++;
                                    }
                                    if(wordCount >= 30)
                                        break;
                                    substringEndIndex++;
                                }
                                wordObj.data.get(url).paragraph = bodyText.substring(substringStartIndex,substringEndIndex);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
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
                            //ignore stop words
                            if (stopWords.contains(currentWord))
                                continue;
                            //add word to the dictionary
                            Word wordObj = addWord(currentWord,stemmer,wordsMap,url);
                            //increment the bold occurrences
                            wordObj.data.get(url).noOccBold++;
                        }
                    }
                    //////////////////////////////////////////////
                    default -> {
                    }
                }
            }
        }
        //calculate the idf and tdf of each word in the words map
        //write to the database
        //getting the HTMLDocuments collection
        MongoCollection<Document> invertedFilesCollection = db.getCollection("InvertedFiles");
        //list of all the documents to be inserted in the database using the insert many query
        List<Document> documentList = new ArrayList<Document>();
        //loop over the words in the map
        for(Map.Entry<String, Word> word : wordsMap.entrySet()){

            ArrayList<Document> dataDocuments = new ArrayList<>();
            //loop over the urls in the word
            for(Map.Entry<String,Data> data : word.getValue().data.entrySet()){
                Document dataDocument = new Document("URL", data.getValue().URL)
                        .append("paragraph", data.getValue().paragraph)
                        .append("TF", data.getValue().TF)
                        .append("IDF", data.getValue().IDF)
                        .append("title", data.getValue().title)
                        .append("noOccBold", data.getValue().noOccBold)
                        .append("H1", data.getValue().H1)
                        .append("H2", data.getValue().H2)
                        .append("H3", data.getValue().H3)
                        .append("H4", data.getValue().H4)
                        .append("H5", data.getValue().H5)
                        .append("H6", data.getValue().H6)
                        .append("date", data.getValue().date)
                        .append("noOccTitle", data.getValue().noOccTitle)
                        .append("titlePosition", data.getValue().titlePosition)
                        .append("bodyPosition", data.getValue().bodyPosition)
                        .append("totalOcc", data.getValue().totalOcc);
                dataDocuments.add(dataDocument);
            }
            Document wordDoc = new Document("word",word.getValue().word).append("data",dataDocuments);
            documentList.add(wordDoc);
            //invertedFilesCollection.insertOne(wordDoc);
        }
        invertedFilesCollection.insertMany(documentList);
        // Close the MongoDB client
        mongoClient.close();
    }
    public static void main(String[] args) throws IOException {
        Indexer myindexer = new Indexer();
        myindexer.runIndexer();
    }
}

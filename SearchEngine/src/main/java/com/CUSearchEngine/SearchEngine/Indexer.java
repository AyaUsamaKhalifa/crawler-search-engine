package com.CUSearchEngine.SearchEngine;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClient;
import com.mongodb.*;
import org.jsoup.select.Elements;

public class Indexer {

    public static void main(String[] args) throws IOException {

        //an array of words which are going to be ignored when parsing the html documents
        String [] stopWords = {"a","about","above","after","again","against","all","am","an","and","any","are","aren't","as","at","be","because","been","before","being","below","between","both","but","by","can't","cannot","could","couldn't",
                "did","didn't","do","does","doesn't","doing","don't","down","during","each","few","for","from","further","had","hadn't","has","hasn't","have","haven't","having","he","he'd","he'll","he's","her","here","here's","hers","herself",
                "him","himself","his","how","how's","i","i'd","i'll","i'm","i've","if","in","into","is","isn't","it","it's","its","itself","let's","me","more","most","mustn't","my","myself","no","nor","not","of","off","on","once","only","or",
                "other","ought","our","ours	ourselves","out","over","own","same","shan't","she","she'd","she'll","she's","should","shouldn't","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there",
                "there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","wasn't","we","we'd","we'll","we're","we've","were","weren't","what","what's","when","when's",
                "where","where's","which","while","who","who's","whom","why","why's","with","won't","would","wouldn't","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"};


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
                    data.put(url,new Data());
                    return false;
                }
                return true;
            }

            static class Data {
                String URL;
                String paragraph;
                int TF;
                int IDF;
                String title;
                int noOccBold;
                int H1;
                int H2;
                int H3;
                int H4;
                int H5;
                int H6;
                int date;
                int noOccTitle;
                int position;

                int totalOcc;


                Data(){
                    noOccTitle = 0;
                    position = 0;
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
        }
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
            //the structure of the html file is mainly the HEAD and the BODY
            //we only care about the title in the head,
            // get the title from the head and parse it
            String titleText = currentHTMLdoc.select("title").text();
            String [] words =titleText.split("\\s+"); //splitting on whitespace
            //parse the title
            for(int i =0;i<words.length;i++){
                String currentWord = words[i];
                //if the word is not included in the words map, create a new word
                if(!wordsMap.containsKey(currentWord)){
                    Word newWord = new Word(currentWord);
                    //insert the word object in the words hashmap
                    wordsMap.put(currentWord,newWord);
                    //add website to the word's data map
                    newWord.addWebsite(url);
                    //increment the number of occurrences in the title
                    newWord.data.get(url).noOccTitle++;
                    //increment the total occurrences
                    newWord.data.get(url).totalOcc++;
                }
                else{
                    Word existingWord = wordsMap.get(currentWord);
                    //add website to the word's data map
                    existingWord.addWebsite(url);
                    //increment the number of occurrences in the title
                    existingWord.data.get(url).noOccTitle++;
                    //increment the total occurrences
                    existingWord.data.get(url).totalOcc++;
                }
            }
            //getting the text of the body and looping over it

            //getting all the html elements
            Elements elements = currentHTMLdoc.getAllElements();
            //looping over the html elements and checking for specific elements from which we
            //want to collect data about the words for the ranker
            for(int i=0; i<elements.size(); i++) {
                String tagName = elements.get(i).tagName();
                switch (tagName) {
                    case "h1":

                        break;
                    case "h2":

                        break;
                    case "h3":

                        break;
                    case "h4":

                        break;
                    case "h5":

                        break;
                    case "h6":

                        break;
                    case "title":

                        break;
                    case "b":

                        break;
                    default:


                }
            }

        }
        // Close the MongoDB client
        mongoClient.close();
    }
}

package com.CUSearchEngine.SearchEngine;
import java.util.*;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Ranker {
    Map<String, Map<String, String>> returnedData; // key => URLs , value => paragraph , title , rank
    Map<String, Double> URLsRank; //URLS and Ranks only
    List <Map.Entry<String, Double>> sortedURLsRank;
    MongoClient mongoClient;
    MongoDatabase db;
    Map<String, String> helperMap; // to test
    //weights to calculate rank
     private double titleOccWeight = 15.0;
     private double boldOccWeight = 8.0;
     private double header1Weight = 6.0;
     private double header2Weight = 5.0;
     private double header3Weight = 4.0;
     private double header4Weight = 3.0;
     private double header5Weight = 2.0;
     private double header6Weight = 1.0;

     public Ranker()
     {
         returnedData = new HashMap<>();
         URLsRank = new HashMap<>();

     }



    private  double rankCalc(double TF, double IDF, double h1, double h2, double h3, double h4, double h5, double h6,double titleOcc,double boldOcc, double popularity)
    {
       double TFAndIDf = TF * IDF;
       return (TFAndIDf + h1 * header1Weight + h2 * header2Weight + h3 * header3Weight + h4 * header4Weight + h5 * header5Weight + h6 *header6Weight + titleOcc * titleOccWeight + boldOcc * boldOccWeight) * popularity;
    }
    public void rank(Map<String, String> word) {

        double TF = Double.parseDouble(word.get("TF"));
        double IDF = Double.parseDouble(word.get("IDF"));
        double boldOcc = Double.parseDouble(word.get("noOccBold"));
        double H1 = Double.parseDouble(word.get("H1"));
        double H2 = Double.parseDouble(word.get("H2"));
        double H3 = Double.parseDouble(word.get("H3"));
        double H4 = Double.parseDouble(word.get("H4"));
        double H5 = Double.parseDouble(word.get("H5"));
        double H6 = Double.parseDouble(word.get("H6"));
        double titleOcc = Double.parseDouble(word.get("noOccTitle"));
        double popularity = Double.parseDouble(word.get("popularity"));
        // Ranking by relevance
        double calculatedRank = rankCalc(TF, IDF, H1, H2, H3, H4, H5, H6, titleOcc, boldOcc, popularity);
        String URL = word.get("URL");
        Map<String, String> currentData = new HashMap<>();

        if (returnedData.containsKey(URL)) {
            double URLRank = URLsRank.get(URL);
            URLsRank.remove(URL);
            URLRank += calculatedRank;
            URLsRank.put(URL, URLRank);
        } else {

            currentData.put("title", word.get("title"));
            currentData.put("paragraph", word.get("paragraph"));
            currentData.put("date", word.get("date"));
            currentData.put("TF", word.get("TF"));
            URLsRank.put(URL, calculatedRank);
            returnedData.put(URL, currentData);
        }
    }
    public void sortData()
    {
        sortedURLsRank = new ArrayList<>(URLsRank.entrySet());
        Collections.sort(sortedURLsRank, Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()));
    }
    public void helper()
    {
        this.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
        this.db = mongoClient.getDatabase("SearchEngine-api-db");
        // Retrieve a value of a key inside a document of documents
        MongoCollection<Document>collection = db.getCollection("InvertedFiles");

        Document query = new Document("word", "facebook");
        Map<String, Object> result = collection.find(query).first();
        helperMap = new HashMap<>();


        // Close the MongoDB client
        for (Map.Entry<String, Object> entry : result.entrySet())
        {
            if(entry.getKey().equalsIgnoreCase("word"))
            {
                System.out.println(entry.getValue());
            }
            else if(entry.getKey().equalsIgnoreCase("data"))
            {
                ArrayList<Map<String, String>> data = (ArrayList<Map<String, String>>) entry.getValue();
                for(Map<String, String> word: data)
                {
                    rank(word);
                    System.out.println(returnedData.get(word.get("URL")).get("title"));
                }
            }
            sortData();
        }
        System.out.println("checking the sort");
        for(Map.Entry<String, Double> link: sortedURLsRank)
        {
            System.out.println(returnedData.get(link.getKey()).get("title"));
        }

        mongoClient.close();

    }
}
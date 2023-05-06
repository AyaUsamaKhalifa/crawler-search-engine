package com.CUSearchEngine.SearchEngine;
import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import org.bson.Document;

import java.util.*;

public class popularity {
    private Map<String, Double> URLPopularity = new HashMap<>(); //hn2rha mn el db 3obara 3n el URL=>key , popularity number=>value
    //private Map<String, ArrayList<String>> URLL = new HashMap<>();
    private float d = 4.0f/10 ; //constant value
    private int iterations = 1000;
//    private MongoClient mongoClient;
//    MongoDatabase db;

//    public void getFromDb()
//    {
//        this.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
//        this.db = mongoClient.getDatabase("SearchEngine-api-db");
//        // Retrieve a value of a key inside a document of documents
//        MongoCollection<Document> collection = db.getCollection("HTMLDocuments");
//        MongoCursor<Document> result = collection.find().cursor();
//
//        while (result.hasNext()) {
//            Document URLS = result.next();
//            URLL.put((String) URLS.get("URL"), (ArrayList<String>) URLS.get("RefIn"));
//        }
//        System.out.println(URLL);
//        calculatePopularity(URLL);
//        System.out.println(URLPopularity);
//    }

    public Map<String, Double> calculatePopularity(Map<String,ArrayList<String>> URL) {
        //give initial value to the urls
        Map<String, Double> popularityTemp = new HashMap<>();
        double initialvalue = 1.0/6000;
        for (String key : URL.keySet())
        {
            URLPopularity.put(key, initialvalue);
        }

        for (int i = 0; i < iterations; i++)  // loop until convergence
        {
            for (String key : URL.keySet()) //loop on all urls
            {
                double popularity = 0;
                for (String value : URL.get(key)) //loop on the urls that refrenced the upper one
                {
                    //get the PR of the URL refrencing the outer url / the number of urls that refrenced the inner one
                    popularity += URLPopularity.get(value).doubleValue() / URL.get(value).size();
                }
                popularity = (1 - d) + d * (popularity);
                popularityTemp.put(key, popularity);
            }
            URLPopularity.putAll(popularityTemp);
        }
        return URLPopularity;
    }
}

package com.CUSearchEngine.SearchEngine;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.ConnectionString;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;



public class Crawler {
    MongoClient mongoClient;
    MongoDatabase db;
    public void ayhabal() {

        this.mongoClient = MongoClients.create(new ConnectionString("mongodb+srv://dbUser:2hMOQwIUAWAK0ymH@cluster0.kn31lqv.mongodb.net"));
        this.db = mongoClient.getDatabase("SearchEngine-api-db");

        db.createCollection("asdfasdf");

    }
}
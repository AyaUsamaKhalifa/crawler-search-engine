package com.CUSearchEngine.SearchEngine;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Config implements CommandLineRunner {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoOperations mongoOps;
    public static Map<String, Double> popularity = new LinkedHashMap<>();
    public static Map<String, Document> URLsConnections = new LinkedHashMap<>();
    public void setPopularity()
    {
        mongoTemplate.findAll( HashMap.class, "URLPopularity")
                .forEach(document -> popularity.put(document.get("url").toString(), (double)document.get("popularity")));
    }
    public void setconnections() throws IOException {
        for(Map.Entry<String, Double> entry : popularity.entrySet())
        {
            String key = entry.getKey();
            Document connections= Jsoup.connect(key).get();
            URLsConnections.put(key,connections);
        }
    }
    @Override
    public void run(String... args) throws Exception {
        setPopularity();
        //setconnections();
    }
}

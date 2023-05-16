package com.CUSearchEngine.SearchEngine;
import java.util.*;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;

public class Ranker {
    Map<String, RankedURLs> returnedData;
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
     }

    private  Integer rankCalc(double TF, double IDF, double h1, double h2, double h3, double h4, double h5, double h6,double titleOcc,double boldOcc, double popularity)
    {
       double TFAndIDf = TF * IDF;
       int rank = (int) Math.round((TFAndIDf + h1 * header1Weight + h2 * header2Weight + h3 * header3Weight + h4 * header4Weight + h5 * header5Weight + h6 *header6Weight + titleOcc * titleOccWeight + boldOcc * boldOccWeight) * popularity);

       return Integer.valueOf(rank);
    }
    public void rank(Website website, String word) {

        double TF = website.TF;
        double IDF = website.IDF;
        double boldOcc = (double) website.noOccBold;
        double H1 = (double) website.H1;
        double H2 = (double)website.H2;
        double H3 = (double) website.H3;
        double H4 = (double) website.H4;
        double H5 = (double) website.H5;
        double H6 = (double) website.H6;
        double titleOcc = (double) website.noOccTitle;

        Integer calculatedRank = rankCalc(TF, IDF, H1, H2, H3, H4, H5, H6, titleOcc, boldOcc, 1);
        String URL = website.URL;

        if (returnedData.containsKey(URL)) {
            RankedURLs URLRank = returnedData.get(URL);
            returnedData.remove(URL);
            URLRank.rank += calculatedRank;
            URLRank.searchedWords.add(word);
            returnedData.put(URL, URLRank);
        } else {
            List<String> words = new ArrayList<>();
            words.add(word);
            RankedURLs URLMapEntry = new RankedURLs(website.title, website.URL, website.paragraph, calculatedRank, words);
            returnedData.put(URL, URLMapEntry);
        }
    }
    public List<RankedURLs> sortData()
    {
        List<RankedURLs> sortedWebsites = new ArrayList<>(returnedData.values());
        Collections.sort(sortedWebsites, new Comparator<RankedURLs>() {
            @Override
            public int compare(RankedURLs o1, RankedURLs o2) {
                return Integer.compare(o2.rank.intValue(), o1.rank.intValue());
            }
        });
        return sortedWebsites;
    }
}
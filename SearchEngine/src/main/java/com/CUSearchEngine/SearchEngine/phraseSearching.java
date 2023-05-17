package com.CUSearchEngine.SearchEngine;

import org.jsoup.Jsoup;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.ArrayList;
import java.util.List;


import static java.lang.Math.log;


public class phraseSearching {

    public List<Website> makingPhraseSearching(String orginalPhrase ,List<Word> Data) throws IOException {
        List<Website> UrlsDataReturned = new ArrayList<Website>();
        //iterate on the urls of the first word
        int min=Integer.MAX_VALUE;
        int minIndex=0;
        //iterate on the urls of the words in the phrase to get the word with the min number of urls
        for (int j=0;j<Data.size();j++)
        {
            if(min>Data.get(j).data.size())
            {
                min=Data.get(j).data.size();
                minIndex=j;
            }
        }
        //iterating on the urls
        for (int j = 0; j < Data.get(minIndex).data.size(); j++)
        {
           // long time1=System.currentTimeMillis();
            String currentUrl = Data.get(minIndex).data.get(j).URL;
            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(currentUrl);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);

            //String body = html.body().text().toLowerCase();
            //String head = html.head().text().toLowerCase();

            int wordCount = 0;
            if  (html.contains(orginalPhrase))
            {
                int index = html.indexOf(orginalPhrase);
                while (index != -1) {
                    wordCount++;
                    index = html.indexOf(orginalPhrase, index + orginalPhrase.length());
                }
            }
//            if (html.contains(orginalPhrase))
//            {
//                int index = html.indexOf(orginalPhrase);
//                while (index != -1) {
//                    wordCount++;
//                    index = head.indexOf(orginalPhrase, index + orginalPhrase.length());
//                }
//            }
            if(wordCount != 0) {
                Website phrase = new Website();
                phrase.TF = (double) wordCount / (double) html.length();
                phrase.title = Data.get(minIndex).data.get(j).title;
                phrase.URL = Data.get(minIndex).data.get(j).URL;
                phrase.paragraph = Data.get(minIndex).data.get(j).paragraph;
                UrlsDataReturned.add(phrase);
            }
        }

        return UrlsDataReturned;
    }
}
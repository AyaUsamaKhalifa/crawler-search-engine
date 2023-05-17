package com.CUSearchEngine.SearchEngine;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;



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

            String currentUrl = Data.get(minIndex).data.get(j).URL;
            Document doc = Jsoup.connect(currentUrl).get();
            String body = doc.body().text().toLowerCase();
            String head = doc.head().text().toLowerCase();

            int wordCount = 0;
            if  (body.contains(orginalPhrase))
            {
                int index = body.indexOf(orginalPhrase);
                while (index != -1) {
                    wordCount++;
                    index = body.indexOf(orginalPhrase, index + orginalPhrase.length());
                }
            }
            if (head.contains(orginalPhrase))
            {
                int index = head.indexOf(orginalPhrase);
                while (index != -1) {
                    wordCount++;
                    index = head.indexOf(orginalPhrase, index + orginalPhrase.length());
                }
            }
            if(wordCount != 0) {
                Website phrase = new Website();
                phrase.TF = (double) wordCount / (double) head.length();
                phrase.title = Data.get(minIndex).data.get(j).title;
                phrase.URL = Data.get(minIndex).data.get(j).URL;
                phrase.paragraph = Data.get(minIndex).data.get(j).paragraph;
                UrlsDataReturned.add(phrase);
            }

        }


        return UrlsDataReturned;
    }
}
package com.CUSearchEngine.SearchEngine;

import org.jsoup.Jsoup;
import java.util.ArrayList;
import java.util.List;


public class phraseSearching {
    public List<Website> makingPhraseSearching(String orginalPhrase ,List<Word> Data) {
        List<Website> UrlsDataReturned = new ArrayList<Website>();
        //iterate on the urls of the first word
        int min=0;
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
            //search in the url
            String title= Jsoup.parse(currentUrl).title();
            System.out.println(Jsoup.parse(currentUrl).body().text());
            if  (title.contains(orginalPhrase))
            {
                UrlsDataReturned.add(Data.get(minIndex).data.get(j));
            }
            else if (Jsoup.parse(currentUrl).body().text().contains(orginalPhrase))
            {
                UrlsDataReturned.add(Data.get(minIndex).data.get(j));
            }
        }
        return UrlsDataReturned;
    }
}
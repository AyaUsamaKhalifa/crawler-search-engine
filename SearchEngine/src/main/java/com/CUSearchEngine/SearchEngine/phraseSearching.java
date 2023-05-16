package com.CUSearchEngine.SearchEngine;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class phraseSearching {
    public List<Website> makingPhraseSearching(String orginalPhrase ,List<Word> Data) throws IOException {
        List<Website> UrlsDataReturned = new ArrayList<Website>();
        //iterate on the urls of the first word
        System.out.println(Data.size());
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
            //search in the url
            //String title= Jsoup.parse(currentUrl).title();
            org.jsoup.nodes.Document currentHTMLdoc = Jsoup.connect(currentUrl).get();
            System.out.println(currentHTMLdoc.body().text());
            System.out.println(currentHTMLdoc.title());
            System.out.println(orginalPhrase);
            if  (currentHTMLdoc.body().text().toLowerCase().contains(orginalPhrase))
            {
                UrlsDataReturned.add(Data.get(minIndex).data.get(j));
            }
            else if (currentHTMLdoc.title().toLowerCase().contains(orginalPhrase))
            {
                UrlsDataReturned.add(Data.get(minIndex).data.get(j));
            }
//            else if (currentHTMLdoc.head().text().contains(orginalPhrase))
//            {
//                UrlsDataReturned.add(Data.get(minIndex).data.get(j));
//            }
        }
        System.out.println("print"+UrlsDataReturned);
        return UrlsDataReturned;

    }
}
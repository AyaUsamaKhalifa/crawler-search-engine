package com.CUSearchEngine.SearchEngine;

import org.jsoup.Jsoup;

import javax.swing.text.Document;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class phraseSearching {
    public ArrayList<Website> makingPhraseSearching(String phrase, ArrayList<Word> Data) {
        ArrayList<Website> UrlsDataReturned = new ArrayList<Website>();
        String[] words = phrase.split(" ");//divide the phrase into words
        //System.out.println(words[0]);
        Map<String, ArrayList<Website>> wordsURLs; //{facebook:[url1,url2,url3]}
        //System.out.println(wordsURLs);
        ArrayList<Integer> postionWord;
        //iterate on the urls of the first word
        for (int j = 0; j < Data.get(0).data.size(); j++) {
            String currentUrl = Data.get(0).data.get(j).URL;
            List<Integer> postions = Data.get(0).data.get(j).bodyPosition;
            int index; //iterate on words of the phrase

            String[] textContent = Jsoup.parse(currentUrl).text().split("[]"); //read the body of the url
            //iterate on the position of the first word in the url
            for (int i = 0; i < postions.size(); i++) {
                index = 1;
                while (index < words.length) {
                    if (textContent[postions.get(i) + index] != words[index]) {
                        break;
                    }
                    index += 1;
                }
                if (index == words.length) {
                    UrlsDataReturned.add(Data.get(0).data.get(j));
                    break;
                }
            }
            postions = Data.get(0).data.get(j).titlePosition;
            for (int i = 0; i < postions.size(); i++) {
                index = 1;
                while (index < words.length) {
                    if (textContent[postions.get(i) + index] != words[index]) {
                        break;
                    }
                    index += 1;
                }
                if (index == words.length) {
                    //if all the words in phrase are matched then add the url to the returned data and
                    //exit the inner loop , you don't need to match the other postions
                    UrlsDataReturned.add(Data.get(0).data.get(j));
                    break;
                }
            }
        }
        return UrlsDataReturned;
    }
}
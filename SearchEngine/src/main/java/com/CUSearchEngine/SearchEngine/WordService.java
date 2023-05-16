package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WordService {
    //It instantiate the class TempRepository
    @Autowired
    //Reference for the repository
    private WordRepository cursor;
    //Access data methods
    public List<RankedURLs> getWord(String word){
        //To Do add the stemmed words



        Optional<Word> currWord =  cursor.findByWord(word);
        Ranker ranker = new Ranker();
        if(currWord.isPresent())
        {
            for(int i = 0; i < currWord.get().getData().size(); i++)
            {
                //To Do change the word to be the stemmed value
                ranker.rank(currWord.get().getData().get(i) ,word);
            }
            List<RankedURLs> rankedWebsites = ranker.sortData();
            for(int i = 0; i < rankedWebsites.size(); i++)
            {
                System.out.println("title" + rankedWebsites.get(i).title);
                System.out.println(rankedWebsites.get(i).rank);
            }
            return rankedWebsites;
        }
        else
        {
            List<RankedURLs> emptyList = new ArrayList<>();
            return emptyList;
        }
    }
}

//        if(currWord.isPresent())
//        {
//            System.out.println(currWord.get().getWord());
//            for(int i = 0; i < currWord.get().getData().size(); i++)
//            {
//                System.out.println("title "+currWord.get().getData().get(i).title);
//                System.out.println("paragraph "+currWord.get().getData().get(i).paragraph);
//                System.out.println(currWord.get().getData().get(i).H1);
//                System.out.println(currWord.get().getData().get(i).H2);
//                System.out.println(currWord.get().getData().get(i).H3);
//                System.out.println(currWord.get().getData().get(i).H4);
//                System.out.println(currWord.get().getData().get(i).H5);
//                System.out.println(currWord.get().getData().get(i).H6);
//                System.out.println(currWord.get().getData().get(i).TF);
//                System.out.println(currWord.get().getData().get(i).IDF);
//                System.out.println(currWord.get().getData().get(i).noOccBold);
//                System.out.println(currWord.get().getData().get(i).noOccTitle);
//                System.out.println(currWord.get().getData().get(i).totalOcc);
//            }
//            return "word successfully found";
//
//        }
//        else
//        {
//            return "word not found";
//        }

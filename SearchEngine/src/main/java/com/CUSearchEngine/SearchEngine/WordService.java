package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tartarus.snowball.ext.porterStemmer;

import java.io.IOException;
import java.util.*;

@Service
public class WordService {
    //It instantiate the class TempRepository
    @Autowired
    //Reference for the repository
    private WordRepository cursor;
    private List<String> stemWords(String searchSentence){
        HashSet<String> stopWords = new HashSet<>(Arrays.asList("a","about","above","after","again","against","all","am","an","and","any","are","aren't","as","at","be","because","been","before","being","below","between","both","but","by","can't","cannot","could","couldn't",
                "did","didn't","do","does","doesn't","doing","don't","down","during","each","few","for","from","further","had","hadn't","has","hasn't","have","haven't","having","he","he'd","he'll","he's","her","here","here's","hers","herself",
                "him","himself","his","how","how's","i","i'd","i'll","i'm","i've","if","in","into","is","isn't","it","it's","its","itself","let's","me","more","most","mustn't","my","myself","no","nor","not","of","off","on","once","only","or",
                "other","ought","our","ours	ourselves","out","over","own","same","shan't","she","she'd","she'll","she's","should","shouldn't","so","some","such","than","that","that's","the","their","theirs","them","themselves","then","there",
                "there's","these","they","they'd","they'll","they're","they've","this","those","through","to","too","under","until","up","very","was","wasn't","we","we'd","we'll","we're","we've","were","weren't","what","what's","when","when's",
                "where","where's","which","while","who","who's","whom","why","why's","with","won't","would","wouldn't","you","you'd","you'll","you're","you've","your","yours","yourself","yourselves"));
        porterStemmer stemmer = new porterStemmer();
        String[] arrOfWords = searchSentence.split("(?=([^\"]*\"[^\"]*\")*[^\"]*$)\\s+", -2);
        List<String> arrOfWordsList = new ArrayList<String>(Arrays.asList(arrOfWords));
        for(int i = 0; i < arrOfWordsList.size(); i++)
        {
            if(!stopWords.contains(arrOfWordsList.get(i)) && arrOfWordsList.get(i).charAt(0) != '\"') {
                stemmer.setCurrent(arrOfWordsList.get(i));
                stemmer.stem();
                arrOfWordsList.set(i, stemmer.getCurrent());
            }
            else if(stopWords.contains(arrOfWordsList.get(i))) {
                arrOfWordsList.remove(i);
            }
        }
        return  arrOfWordsList;
    }
    //Access data methods
    public List<RankedURLs> getWord(String word) throws IOException {
        //To Do add the stemmed words
        List<String> words = stemWords(word);
        Ranker ranker = new Ranker();
        for (int i = 0; i < words.size(); i++) {
            //System.out.println("word "+words.get(i));
            if(words.get(i).charAt(0) != '\"') {
                System.out.println(words.get(i).charAt(0));
                Optional<Word> currWord = cursor.findByWord(words.get(i).toLowerCase());
                if (currWord.isPresent()) {
                    System.out.println("word " + currWord.get().word);
                    for (int j = 0; j < currWord.get().getData().size(); j++) {
                        //To Do change the word to be the stemmed value
                        ranker.rank(currWord.get().getData().get(j), words.get(i).toLowerCase());
                    }
                }
            }
            else
            {
                phraseSearching phrase = new phraseSearching();
                String originalPhrase = words.get(i).replace("\"", "");
                String copy = String.valueOf(originalPhrase);
                List<String> phraseData = new ArrayList<String>(stemWords(copy));
                List<Word> websites = new ArrayList<>();
                for(int k = 0; k < phraseData.size(); k++)
                {
                    Optional<Word> currWord = cursor.findByWord(phraseData.get(k).toLowerCase());
                    if(currWord.isPresent())
                    {
                        websites.add(currWord.get());
                    }

                }
                System.out.println(websites);
                List<Website>resultedWebsites = phrase.makingPhraseSearching(originalPhrase, websites);
                for (int j = 0; j < resultedWebsites.size(); j++) {
                    //To Do change the word to be the stemmed value
                    ranker.rank(resultedWebsites.get(j), '\"' + originalPhrase.toLowerCase()+'\"');
                }
            }
        }
        List<RankedURLs> rankedWebsites = ranker.sortData();
        for (int i = 0; i < rankedWebsites.size(); i++) {
            System.out.println("title" + rankedWebsites.get(i).title);
            System.out.println(rankedWebsites.get(i).rank);
        }
        return rankedWebsites;
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

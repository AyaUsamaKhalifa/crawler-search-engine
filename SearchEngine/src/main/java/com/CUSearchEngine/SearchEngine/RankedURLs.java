package com.CUSearchEngine.SearchEngine;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RankedURLs {
    @Id
    @JsonProperty("id")
    private String id = UUID.randomUUID().toString();;
    @JsonProperty("title")
    String title;

    @JsonProperty("link")
    String link;

    @JsonProperty("paragraph")
    String paragraph;

    @JsonProperty("rank")
    Integer rank;

    @JsonProperty("searchedWords")
    List<String> searchedWords;

    public RankedURLs(String title, String link, String paragraph, Integer Rank, List<String> words) {
        this.title = title;
        this.link = link;
        this.paragraph = paragraph;
        this.rank = Rank;
        this.searchedWords = words;
    }
}

package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.DocumentOperators;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/SearchResults")
public class WordController {
    // Service reference
    @Autowired
    private WordService wordService;
    @GetMapping("/{SearchString}")
    public ResponseEntity<List<RankedURLs>> getWebsites(@PathVariable(value = "SearchString") String SearchedString) throws IOException {
        return new ResponseEntity<List<RankedURLs>>(wordService.getWord(SearchedString), HttpStatus.OK);
    }
}
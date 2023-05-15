package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/PopularSearches")
public class RecommendationController {

    // Service reference
    @Autowired
    private RecommendationService recommendationService;
    @GetMapping
    public ResponseEntity<List<Recommendation>> getAllRecommendations(){
        return new ResponseEntity<List<Recommendation>>(recommendationService.allRecommendations(), HttpStatus.OK);
    }
    @PostMapping()
    public ResponseEntity<Recommendation> testRequest(@RequestBody Map<String, String> payload){
        return new ResponseEntity<Recommendation>(recommendationService.updateRecommendation(payload.get("title")), HttpStatus.OK);
    }



}

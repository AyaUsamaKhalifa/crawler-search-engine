package com.CUSearchEngine.SearchEngine;

import com.mongodb.client.model.Sorts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mongodb.client.model.Sorts.descending;

@Service
public class RecommendationService {
    //It instantiate the class TempRepository
    @Autowired
    //Reference for the repository
    private RecommendationRepository cursor;
    //Access data methods
    public List<Recommendation> allRecommendations(){
        // findAll --> in mongo repository class it will return a list of Temp
        return cursor.findAll(Sort.by(Sort.Direction.DESC, "noOfClicks"));
    }
}

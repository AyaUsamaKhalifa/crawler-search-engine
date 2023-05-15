package com.CUSearchEngine.SearchEngine;

import com.mongodb.ReadConcern;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.model.Sorts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.management.Query;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;

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
    @Transactional
    public Recommendation updateRecommendation(String title){

        String tempTitle = title.substring(0,1).toUpperCase() + title.substring(1).toLowerCase();
        Optional<Recommendation> titleObj= cursor.findRecommendationByTitle(tempTitle);
        if(titleObj.isPresent())
        {
            Integer clicks = titleObj.get().getNoOfClicks();
            titleObj.get().setNoOfClicks(clicks+1);
            cursor.save(titleObj.get());
            return titleObj.get();
        }
        else
        {
            Recommendation newRecommendation = new Recommendation(tempTitle);
            cursor.insert(newRecommendation);
            return newRecommendation;
        }
    }
}

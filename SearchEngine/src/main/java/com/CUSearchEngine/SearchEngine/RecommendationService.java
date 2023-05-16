package com.CUSearchEngine.SearchEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;


@Service
public class RecommendationService {
    //It instantiate the class TempRepository
    @Autowired
    //Reference for the repository
    private RecommendationRepository cursor;
    //Access data methods
    public List<Recommendation> allRecommendations(){
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

package com.CUSearchEngine.SearchEngine;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, ObjectId> {
    Optional<Recommendation> findRecommendationByTitle(String Title);
}

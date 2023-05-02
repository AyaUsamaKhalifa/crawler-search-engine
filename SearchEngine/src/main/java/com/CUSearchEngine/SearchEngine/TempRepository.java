package com.CUSearchEngine.SearchEngine;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TempRepository extends MongoRepository<Temp, ObjectId> {
}

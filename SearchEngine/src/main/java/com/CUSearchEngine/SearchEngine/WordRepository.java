package com.CUSearchEngine.SearchEngine;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends MongoRepository<Word, ObjectId> {
    Optional<Word> findByWord(String word);
}

package com.CUSearchEngine.SearchEngine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Document(collection = "InvertedFiles")
@CrossOrigin(origins = "*", allowedHeaders = "*")
//Handles getters and setters
@Data
//Creating a constructor that takes all private fields as parameters
@AllArgsConstructor
//A constructor that takes no parameters
@NoArgsConstructor
public class Word {
    @Id
    public ObjectId id;
    public String word;
    public double IDF;
    public List<Website> data;
}

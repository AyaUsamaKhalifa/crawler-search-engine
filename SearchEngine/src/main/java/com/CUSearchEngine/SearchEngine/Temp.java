package com.CUSearchEngine.SearchEngine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Temp")
//Handles getters and setters
@Data
//Creating a constructor that takes all private fields as parameters
@AllArgsConstructor
//A constructor that takes no parameters
@NoArgsConstructor
public class Temp {
    @Id
    private ObjectId id;
    private String imdbId;
    private String Name;


}

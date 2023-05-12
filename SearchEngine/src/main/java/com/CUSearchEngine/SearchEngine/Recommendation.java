package com.CUSearchEngine.SearchEngine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "PopularSearches")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    @Id
    private ObjectId id;
    private String title;
    private int noOfClicks;
}

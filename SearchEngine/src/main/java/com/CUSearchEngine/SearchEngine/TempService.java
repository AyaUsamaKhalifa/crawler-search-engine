package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TempService {
    //It instantiate the class TempRepository
    @Autowired
    //Reference for the repository
    private TempRepository tempRepo;
    //Access data methods
    public List<Temp> allMovies(){
        // findAll --> in mongo repository class it will return a list of Temp
        return tempRepo.findAll();
    }
}

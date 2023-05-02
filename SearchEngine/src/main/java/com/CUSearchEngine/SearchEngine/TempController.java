package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/Temp")
public class TempController {
    // Service reference
    @Autowired
    private TempService tempService;
    @GetMapping
    public ResponseEntity<List<Temp>> getAllMovies(){
        return new ResponseEntity<List<Temp>>(tempService.allMovies(), HttpStatus.OK);
    }
}
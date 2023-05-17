package com.CUSearchEngine.SearchEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication

public class SearchEngineApplication {

	public static void main(String[] args) {


		SpringApplication.run(SearchEngineApplication.class, args);
		//phraseSearching ps=new phraseSearching();
		//ps.makingPhraseSearching("facebook geeks");


	}

}

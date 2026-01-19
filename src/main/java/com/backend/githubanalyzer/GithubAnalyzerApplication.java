package com.backend.githubanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableAsync
@SpringBootApplication
public class GithubAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubAnalyzerApplication.class, args);
    }

}

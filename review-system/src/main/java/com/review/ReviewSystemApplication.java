package com.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReviewSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewSystemApplication.class, args);
    }
}

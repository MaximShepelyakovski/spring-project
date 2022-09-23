package com.softkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StarterApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(StarterApplication.class, args);
    }

}

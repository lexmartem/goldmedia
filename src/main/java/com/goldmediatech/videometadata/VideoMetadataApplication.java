package com.goldmediatech.videometadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Video Metadata Service Application
 * 
 * A Spring Boot application for managing and analyzing video metadata
 * from mock sources for development and testing purposes.
 */
@SpringBootApplication
@EnableScheduling
public class VideoMetadataApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoMetadataApplication.class, args);
    }
} 
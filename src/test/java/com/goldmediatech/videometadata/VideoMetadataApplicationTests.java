package com.goldmediatech.videometadata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test for the Video Metadata Service application.
 * 
 * This test verifies that the Spring Boot application context loads correctly
 * with all required beans and configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
class VideoMetadataApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
        // It verifies that all required beans are properly configured
    }
} 
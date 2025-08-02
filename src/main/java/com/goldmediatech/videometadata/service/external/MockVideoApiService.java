package com.goldmediatech.videometadata.service.external;

import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock video API service for development and testing.
 * 
 * This service generates realistic video metadata for testing purposes
 * without requiring external API dependencies.
 */
@Service
public class MockVideoApiService implements VideoApiService {

    private static final Logger logger = LoggerFactory.getLogger(MockVideoApiService.class);

    @Value("${app.external-apis.mock.enabled:true}")
    private boolean enabled;

    @Value("${app.external-apis.mock.timeout:2000}")
    private int timeout;

    private static final String[] VIDEO_TITLES = {
        "Introduction to Spring Boot", "Advanced Java Programming", "Microservices Architecture",
        "Docker Containerization", "Kubernetes Orchestration", "REST API Design",
        "Database Design Patterns", "Cloud Computing Basics", "DevOps Best Practices",
        "Security in Web Applications", "Performance Optimization", "Testing Strategies",
        "CI/CD Pipeline Setup", "Monitoring and Logging", "API Gateway Patterns"
    };

    private static final String[] VIDEO_DESCRIPTIONS = {
        "Learn the fundamentals of Spring Boot framework and build robust applications.",
        "Master advanced Java concepts including generics, streams, and concurrency.",
        "Explore microservices architecture patterns and implementation strategies.",
        "Understand Docker containerization and deployment best practices.",
        "Learn Kubernetes orchestration for scalable container deployments.",
        "Design and implement RESTful APIs following industry standards.",
        "Explore database design patterns for optimal performance and scalability.",
        "Introduction to cloud computing concepts and service models.",
        "Implement DevOps practices for continuous delivery and deployment.",
        "Secure your web applications with industry best practices.",
        "Optimize application performance for better user experience.",
        "Comprehensive testing strategies for reliable software delivery.",
        "Set up continuous integration and deployment pipelines.",
        "Implement effective monitoring and logging solutions.",
        "Design and implement API gateway patterns for microservices."
    };

    @Override
    public Optional<Video> getVideoMetadata(String videoId) {
        if (!enabled) {
            logger.warn("Mock video API service is disabled");
            return Optional.empty();
        }

        try {
            // Simulate API delay
            Thread.sleep(new Random().nextInt(Math.max(1, timeout - 100)) + 100);
            
            // Simulate occasional failures (5% failure rate)
            if (new Random().nextDouble() < 0.05) {
                logger.warn("Mock API failure for video ID: {}", videoId);
                return Optional.empty();
            }

            Video video = generateMockVideo(videoId);
            logger.debug("Generated mock video metadata for ID: {}", videoId);
            
            return Optional.of(video);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Mock API interrupted for video ID: {}", videoId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error generating mock video metadata for ID: {} - {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Video> getVideoMetadataBatch(List<String> videoIds) {
        if (!enabled) {
            logger.warn("Mock video API service is disabled");
            return Collections.emptyList();
        }

        List<Video> videos = new ArrayList<>();
        
        for (String videoId : videoIds) {
            Optional<Video> video = getVideoMetadata(videoId);
            videos.add(video.orElse(null));
        }
        
        logger.info("Processed batch of {} video IDs, generated {} videos", 
                   videoIds.size(), videos.stream().filter(Objects::nonNull).count());
        
        return videos;
    }

    @Override
    public VideoSource getSource() {
        return VideoSource.MOCK;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    /**
     * Generate mock video metadata for testing.
     * 
     * @param videoId the video ID
     * @return Video entity with mock data
     */
    private Video generateMockVideo(String videoId) {
        Random random = new Random();
        
        String title = VIDEO_TITLES[random.nextInt(VIDEO_TITLES.length)];
        String description = VIDEO_DESCRIPTIONS[random.nextInt(VIDEO_DESCRIPTIONS.length)];
        
        // Generate random duration between 60 and 3600 seconds (1-60 minutes)
        int duration = random.nextInt(3540) + 60;
        
        // Generate random upload date within last 2 years
        LocalDateTime uploadDate = LocalDateTime.now().minusDays(random.nextInt(365));
        
        // Generate thumbnail and embed URLs
        String thumbnailUrl = String.format("https://mock.example.com/thumbnails/%s.jpg", videoId);
        String embedUrl = String.format("https://mock.example.com/embed/%s", videoId);
        
        // Generate metadata as JSON string
        String metadata = String.format(
            "{\"viewCount\":%d,\"likeCount\":%d,\"commentCount\":%d,\"category\":\"Education\",\"language\":\"en\",\"quality\":\"%s\"}",
            random.nextInt(1000000),
            random.nextInt(10000),
            random.nextInt(1000),
            random.nextBoolean() ? "HD" : "SD"
        );
        
        Video video = new Video(videoId, title, VideoSource.MOCK);
        video.setDescription(description);
        video.setDuration(duration);
        video.setThumbnailUrl(thumbnailUrl);
        video.setEmbedUrl(embedUrl);
        video.setUploadDate(uploadDate);
        video.setMetadata(metadata);
        
        return video;
    }
} 
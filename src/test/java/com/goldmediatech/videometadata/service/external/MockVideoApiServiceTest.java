package com.goldmediatech.videometadata.service.external;

import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MockVideoApiService.
 * 
 * This test class provides comprehensive coverage for the mock video API service
 * that generates realistic video metadata for testing purposes.
 */
@ExtendWith(MockitoExtension.class)
class MockVideoApiServiceTest {

    @InjectMocks
    private MockVideoApiService mockVideoApiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mockVideoApiService, "enabled", true);
        ReflectionTestUtils.setField(mockVideoApiService, "timeout", 100);
    }

    @Test
    void getVideoMetadata_WhenEnabled_ShouldReturnVideo() {
        // When
        Optional<Video> result = mockVideoApiService.getVideoMetadata("test-video-1");

        // Then
        assertTrue(result.isPresent());
        Video video = result.get();
        assertEquals("test-video-1", video.getVideoId());
        assertEquals(VideoSource.MOCK, video.getSource());
        assertNotNull(video.getTitle());
        assertNotNull(video.getDescription());
        assertTrue(video.getDuration() >= 60 && video.getDuration() <= 3600);
        assertNotNull(video.getThumbnailUrl());
        assertNotNull(video.getEmbedUrl());
        assertNotNull(video.getUploadDate());
        assertNotNull(video.getMetadata());
    }

    @Test
    void getVideoMetadata_WhenDisabled_ShouldReturnEmpty() {
        // Given
        ReflectionTestUtils.setField(mockVideoApiService, "enabled", false);

        // When
        Optional<Video> result = mockVideoApiService.getVideoMetadata("test-video-1");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getVideoMetadata_WithDifferentIds_ShouldGenerateDifferentVideos() {
        // When
        Optional<Video> video1 = mockVideoApiService.getVideoMetadata("video-1");
        Optional<Video> video2 = mockVideoApiService.getVideoMetadata("video-2");

        // Then
        assertTrue(video1.isPresent());
        assertTrue(video2.isPresent());
        assertEquals("video-1", video1.get().getVideoId());
        assertEquals("video-2", video2.get().getVideoId());
        // The titles might be the same due to random selection from a small array
        // But the video IDs should be different
        assertNotEquals(video1.get().getVideoId(), video2.get().getVideoId());
    }

    @Test
    void getVideoMetadataBatch_WhenEnabled_ShouldReturnVideos() {
        // Given
        List<String> videoIds = Arrays.asList("video-1", "video-2", "video-3");

        // When
        List<Video> result = mockVideoApiService.getVideoMetadataBatch(videoIds);

        // Then
        assertEquals(3, result.size());
        assertNotNull(result.get(0));
        assertNotNull(result.get(1));
        assertNotNull(result.get(2));
        assertEquals("video-1", result.get(0).getVideoId());
        assertEquals("video-2", result.get(1).getVideoId());
        assertEquals("video-3", result.get(2).getVideoId());
    }

    @Test
    void getVideoMetadataBatch_WhenDisabled_ShouldReturnEmptyList() {
        // Given
        ReflectionTestUtils.setField(mockVideoApiService, "enabled", false);
        List<String> videoIds = Arrays.asList("video-1", "video-2");

        // When
        List<Video> result = mockVideoApiService.getVideoMetadataBatch(videoIds);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getVideoMetadataBatch_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        List<String> videoIds = Arrays.asList();

        // When
        List<Video> result = mockVideoApiService.getVideoMetadataBatch(videoIds);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getSource_ShouldReturnMock() {
        // When
        VideoSource source = mockVideoApiService.getSource();

        // Then
        assertEquals(VideoSource.MOCK, source);
    }

    @Test
    void isAvailable_WhenEnabled_ShouldReturnTrue() {
        // When
        boolean available = mockVideoApiService.isAvailable();

        // Then
        assertTrue(available);
    }

    @Test
    void isAvailable_WhenDisabled_ShouldReturnFalse() {
        // Given
        ReflectionTestUtils.setField(mockVideoApiService, "enabled", false);

        // When
        boolean available = mockVideoApiService.isAvailable();

        // Then
        assertFalse(available);
    }

    @Test
    void getVideoMetadata_ShouldGenerateRealisticData() {
        // When
        Optional<Video> result = mockVideoApiService.getVideoMetadata("test-video");

        // Then
        assertTrue(result.isPresent());
        Video video = result.get();
        
        // Check title is from predefined list
        String[] expectedTitles = {
            "Introduction to Spring Boot", "Advanced Java Programming", "Microservices Architecture",
            "Docker Containerization", "Kubernetes Orchestration", "REST API Design",
            "Database Design Patterns", "Cloud Computing Basics", "DevOps Best Practices",
            "Security in Web Applications", "Performance Optimization", "Testing Strategies",
            "CI/CD Pipeline Setup", "Monitoring and Logging", "API Gateway Patterns"
        };
        assertTrue(Arrays.asList(expectedTitles).contains(video.getTitle()));
        
        // Check duration is within reasonable range
        assertTrue(video.getDuration() >= 60 && video.getDuration() <= 3600);
        
        // Check thumbnail URL format
        assertTrue(video.getThumbnailUrl().startsWith("https://mock.example.com/thumbnails/"));
        assertTrue(video.getThumbnailUrl().endsWith(".jpg"));
        
        // Check embed URL format
        assertTrue(video.getEmbedUrl().startsWith("https://mock.example.com/embed/"));
        
        // Check metadata contains expected fields
        String metadata = video.getMetadata();
        assertTrue(metadata.contains("viewCount"));
        assertTrue(metadata.contains("likeCount"));
        assertTrue(metadata.contains("commentCount"));
        assertTrue(metadata.contains("category"));
        assertTrue(metadata.contains("language"));
        assertTrue(metadata.contains("quality"));
    }



    @Test
    void getVideoMetadata_ShouldOccasionallyFail() {
        // Given - We'll test multiple times to catch the 5% failure rate
        int successCount = 0;
        int failureCount = 0;
        
        // When
        for (int i = 0; i < 100; i++) {
            Optional<Video> result = mockVideoApiService.getVideoMetadata("test-video-" + i);
            if (result.isPresent()) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        // Then
        // Should have mostly successes with some failures (around 5% failure rate)
        assertTrue(successCount > 90); // At least 90% success
        assertTrue(failureCount > 0); // Some failures expected
        assertTrue(failureCount < 20); // But not too many failures
    }

    @Test
    void getVideoMetadata_ShouldGenerateDifferentMetadataForDifferentVideos() {
        // When
        Optional<Video> video1 = mockVideoApiService.getVideoMetadata("video-1");
        Optional<Video> video2 = mockVideoApiService.getVideoMetadata("video-2");

        // Then
        assertTrue(video1.isPresent());
        assertTrue(video2.isPresent());
        
        // Video IDs should be different
        assertNotEquals(video1.get().getVideoId(), video2.get().getVideoId());
        
        // Duration should be different (due to random generation)
        // Note: Metadata might be the same due to random selection from small arrays
        assertNotEquals(video1.get().getDuration(), video2.get().getDuration());
    }
} 
package com.goldmediatech.videometadata.service.external;

import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;

import java.util.List;
import java.util.Optional;

/**
 * Interface for external video API services.
 * 
 * This interface defines the contract for integrating with external video
 * platforms. Currently supports mock services for development and testing.
 */
public interface VideoApiService {

    /**
     * Get video metadata by video ID.
     * 
     * @param videoId the external video ID
     * @return Optional containing video metadata if found
     */
    Optional<Video> getVideoMetadata(String videoId);

    /**
     * Get video metadata for multiple video IDs.
     * 
     * @param videoIds list of video IDs
     * @return list of video metadata (may contain null values for failed requests)
     */
    List<Video> getVideoMetadataBatch(List<String> videoIds);

    /**
     * Get the source platform this service handles.
     * 
     * @return VideoSource enum value
     */
    VideoSource getSource();

    /**
     * Check if the service is available/enabled.
     * 
     * @return true if service is available, false otherwise
     */
    boolean isAvailable();
} 
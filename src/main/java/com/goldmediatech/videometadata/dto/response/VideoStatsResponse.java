package com.goldmediatech.videometadata.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for video statistics responses.
 * 
 * This class represents the response body for video analytics and statistics,
 * including counts, durations, and source breakdowns.
 */
public record VideoStatsResponse(
    @JsonProperty("total_videos")
    Long totalVideos,

    @JsonProperty("videos_by_source")
    Map<String, Long> videosBySource,

    @JsonProperty("average_duration_by_source")
    Map<String, Double> averageDurationBySource,

    @JsonProperty("total_duration")
    Long totalDuration,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("last_updated")
    LocalDateTime lastUpdated
) {
    public VideoStatsResponse {
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
} 
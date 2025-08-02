package com.goldmediatech.videometadata.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for video import operation responses.
 * 
 * This class represents the response body for bulk video import operations,
 * including success/failure counts and details.
 */
public record ImportResponse(
    @JsonProperty("total_requested")
    Integer totalRequested,

    @JsonProperty("successful_imports")
    Integer successfulImports,

    @JsonProperty("failed_imports")
    Integer failedImports,

    @JsonProperty("skipped_imports")
    Integer skippedImports,

    @JsonProperty("failed_video_ids")
    List<String> failedVideoIds,

    @JsonProperty("skipped_video_ids")
    List<String> skippedVideoIds,

    @JsonProperty("source")
    String source,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("import_timestamp")
    LocalDateTime importTimestamp,

    @JsonProperty("processing_time_ms")
    Long processingTimeMs
) {
    public ImportResponse {
        if (importTimestamp == null) {
            importTimestamp = LocalDateTime.now();
        }
    }
} 
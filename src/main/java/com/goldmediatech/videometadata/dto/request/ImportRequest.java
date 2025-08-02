package com.goldmediatech.videometadata.dto.request;

import com.goldmediatech.videometadata.entity.VideoSource;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * DTO for video import requests.
 * 
 * This class represents the request body for bulk video import operations
 * from external APIs.
 */
public record ImportRequest(
    @NotNull(message = "Source is required")
    VideoSource source,

    @NotEmpty(message = "Video IDs list cannot be empty")
    @Size(max = 100, message = "Maximum 100 video IDs allowed per request")
    List<@NotBlank(message = "Video ID cannot be blank") String> videoIds,

    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 100, message = "Batch size cannot exceed 100")
    Integer batchSize
) {
    // Default value for batchSize
    public ImportRequest {
        if (batchSize == null) {
            batchSize = 10;
        }
    }
} 
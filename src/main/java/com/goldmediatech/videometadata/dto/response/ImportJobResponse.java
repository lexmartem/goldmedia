package com.goldmediatech.videometadata.dto.response;

import com.goldmediatech.videometadata.entity.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for import job operations.
 */
@Schema(description = "Import job response")
public record ImportJobResponse(
    @Schema(description = "Unique job identifier")
    String jobId,
    
    @Schema(description = "Job status")
    ImportStatus status,
    
    @Schema(description = "Response message")
    String message,
    
    @Schema(description = "Job creation timestamp")
    LocalDateTime createdAt,
    
    @Schema(description = "Job start timestamp")
    LocalDateTime startedAt,
    
    @Schema(description = "Job completion timestamp")
    LocalDateTime completedAt,
    
    @Schema(description = "Error message if job failed")
    String errorMessage,
    
    @Schema(description = "Import result if job completed")
    ImportResponse result
) {
    
    /**
     * Create a response for job creation.
     */
    public static ImportJobResponse jobCreated(String jobId, LocalDateTime createdAt) {
        return new ImportJobResponse(
            jobId,
            ImportStatus.PENDING,
            "Import job created successfully",
            createdAt,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Create a response for job status.
     */
    public static ImportJobResponse fromJob(String jobId, ImportStatus status, String message,
                                          LocalDateTime createdAt, LocalDateTime startedAt,
                                          LocalDateTime completedAt, String errorMessage,
                                          ImportResponse result) {
        return new ImportJobResponse(
            jobId,
            status,
            message,
            createdAt,
            startedAt,
            completedAt,
            errorMessage,
            result
        );
    }
} 
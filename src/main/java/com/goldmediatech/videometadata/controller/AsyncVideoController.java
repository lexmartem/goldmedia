package com.goldmediatech.videometadata.controller;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportJobResponse;
import com.goldmediatech.videometadata.entity.ImportJob;
import com.goldmediatech.videometadata.entity.ImportStatus;
import com.goldmediatech.videometadata.service.AsyncVideoImportService;
import com.goldmediatech.videometadata.service.ImportJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for asynchronous video import operations.
 * 
 * This controller provides endpoints for background video import processing
 * with job tracking and status monitoring.
 */
@RestController
@RequestMapping("/videos/import")
@Tag(name = "Async Video Import", description = "Asynchronous video import endpoints")
public class AsyncVideoController {

    private static final Logger logger = LoggerFactory.getLogger(AsyncVideoController.class);

    @Autowired
    private AsyncVideoImportService asyncVideoImportService;

    @Autowired
    private ImportJobService importJobService;

    /**
     * Start an asynchronous video import job.
     * 
     * @param importRequest the import request
     * @return ResponseEntity with job creation response
     */
    @PostMapping("/async")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Start async video import",
        description = "Start a background video import job (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "Import job started successfully",
            content = @Content(
                schema = @Schema(implementation = ImportJobResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required"
        )
    })
    public ResponseEntity<ImportJobResponse> startAsyncImport(@Valid @RequestBody ImportRequest importRequest) {
        logger.info("Starting async video import for source: {}, video IDs: {}", 
                   importRequest.source(), importRequest.videoIds());

        try {
            // Generate job ID
            String jobId = "import-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            // Start async processing with the job ID
            asyncVideoImportService.startAsyncImport(importRequest, jobId);
            
            // Return immediate response
            ImportJobResponse response = ImportJobResponse.jobCreated(jobId, LocalDateTime.now());
            
            logger.info("Async import job started: {}", jobId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to start async import: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get import job status by ID.
     * 
     * @param jobId the job ID
     * @return ResponseEntity with job status
     */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get import job status",
        description = "Get the status and details of an import job",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job status retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = ImportJobResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - USER or ADMIN role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Job not found"
        )
    })
    public ResponseEntity<ImportJobResponse> getJobStatus(
            @Parameter(description = "Job ID")
            @PathVariable String jobId) {

        logger.debug("Getting status for job: {}", jobId);
        
        Optional<ImportJob> jobOpt = importJobService.getJob(jobId);
        
        if (jobOpt.isPresent()) {
            ImportJob job = jobOpt.get();
            ImportJobResponse response = ImportJobResponse.fromJob(
                job.getJobId(),
                job.getStatus(),
                getStatusMessage(job.getStatus()),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getErrorMessage(),
                job.getResult()
            );
            
            logger.debug("Job status retrieved: {} - {}", jobId, job.getStatus());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Job not found: {}", jobId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all import jobs.
     * 
     * @return ResponseEntity with list of all jobs
     */
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get all import jobs",
        description = "Get a list of all import jobs",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Jobs retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - USER or ADMIN role required"
        )
    })
    public ResponseEntity<List<ImportJob>> getAllJobs() {
        logger.debug("Getting all import jobs");
        
        List<ImportJob> jobs = importJobService.getAllJobs();
        logger.debug("Retrieved {} import jobs", jobs.size());
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get import jobs by status.
     * 
     * @param status the job status to filter by
     * @return ResponseEntity with filtered jobs
     */
    @GetMapping("/jobs/status/{status}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get jobs by status",
        description = "Get import jobs filtered by status",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Jobs retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - USER or ADMIN role required"
        )
    })
    public ResponseEntity<List<ImportJob>> getJobsByStatus(
            @Parameter(description = "Job status")
            @PathVariable ImportStatus status) {

        logger.debug("Getting jobs with status: {}", status);
        
        List<ImportJob> jobs = importJobService.getJobsByStatus(status);
        logger.debug("Retrieved {} jobs with status {}", jobs.size(), status);
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Cancel an import job.
     * 
     * @param jobId the job ID to cancel
     * @return ResponseEntity with cancellation result
     */
    @DeleteMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cancel import job",
        description = "Cancel a running or pending import job (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job cancelled successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Job cannot be cancelled"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Job not found"
        )
    })
    public ResponseEntity<String> cancelJob(
            @Parameter(description = "Job ID")
            @PathVariable String jobId) {

        logger.info("Attempting to cancel job: {}", jobId);
        
        boolean cancelled = importJobService.cancelJob(jobId);
        
        if (cancelled) {
            logger.info("Successfully cancelled job: {}", jobId);
            return ResponseEntity.ok("Job cancelled successfully");
        } else {
            logger.warn("Failed to cancel job: {} - job not found or already completed", jobId);
            return ResponseEntity.badRequest().body("Job cannot be cancelled");
        }
    }

    /**
     * Get job statistics.
     * 
     * @return ResponseEntity with job statistics
     */
    @GetMapping("/jobs/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get job statistics",
        description = "Get statistics about import jobs",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - USER or ADMIN role required"
        )
    })
    public ResponseEntity<ImportJobService.JobStatistics> getJobStatistics() {
        logger.debug("Getting job statistics");
        
        ImportJobService.JobStatistics stats = importJobService.getJobStatistics();
        logger.debug("Retrieved job statistics: {}", stats);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get status message for a job status.
     * 
     * @param status the job status
     * @return status message
     */
    private String getStatusMessage(ImportStatus status) {
        return switch (status) {
            case PENDING -> "Job is pending execution";
            case RUNNING -> "Job is currently running";
            case COMPLETED -> "Job completed successfully";
            case FAILED -> "Job failed with an error";
            case CANCELLED -> "Job was cancelled";
        };
    }
} 
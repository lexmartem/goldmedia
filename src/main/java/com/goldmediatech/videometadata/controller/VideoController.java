package com.goldmediatech.videometadata.controller;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportResponse;
import com.goldmediatech.videometadata.dto.response.VideoStatsResponse;
import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.service.VideoService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Video controller for handling video-related HTTP requests.
 * 
 * This controller provides endpoints for video import, querying, and statistics
 * with proper authentication and authorization.
 */
@RestController
@RequestMapping("/videos")
@Tag(name = "Videos", description = "Video management endpoints")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;

    /**
     * Import videos from external API.
     * 
     * @param importRequest the import request
     * @return ResponseEntity with import results
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Import videos",
        description = "Import video metadata from external APIs (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Import completed successfully",
            content = @Content(
                schema = @Schema(implementation = ImportResponse.class)
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
    public ResponseEntity<ImportResponse> importVideos(@Valid @RequestBody ImportRequest importRequest) {
        logger.info("Video import request received - Source: {}, Video IDs: {}", 
                   importRequest.source(), importRequest.videoIds());
        
        try {
            ImportResponse response = videoService.importVideos(importRequest);
            logger.info("Video import completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Video import failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get videos with filters and pagination.
     * 
     * @param source video source filter
     * @param uploadDateFrom start date filter
     * @param uploadDateTo end date filter
     * @param minDuration minimum duration filter
     * @param maxDuration maximum duration filter
     * @param page page number (0-based)
     * @param size page size
     * @param sort sort field
     * @param direction sort direction
     * @return ResponseEntity with paginated videos
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get videos",
        description = "Get paginated list of videos with optional filters",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Videos retrieved successfully"
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
    public ResponseEntity<Page<Video>> getVideos(
            @Parameter(description = "Video source filter")
            @RequestParam(required = false) VideoSource source,
            
            @Parameter(description = "Upload date from (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime uploadDateFrom,
            
            @Parameter(description = "Upload date to (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime uploadDateTo,
            
            @Parameter(description = "Minimum duration in seconds")
            @RequestParam(required = false) Integer minDuration,
            
            @Parameter(description = "Maximum duration in seconds")
            @RequestParam(required = false) Integer maxDuration,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sort,
            
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        logger.debug("Get videos request - source: {}, dateFrom: {}, dateTo: {}, duration: {}-{}, page: {}, size: {}",
                   source, uploadDateFrom, uploadDateTo, minDuration, maxDuration, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        
        Page<Video> videos = videoService.findVideos(source, uploadDateFrom, uploadDateTo, 
                                                    minDuration, maxDuration, pageable);
        
        logger.debug("Retrieved {} videos out of {} total", videos.getContent().size(), videos.getTotalElements());
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Get video by ID.
     * 
     * @param id the video ID
     * @return ResponseEntity with video details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get video by ID",
        description = "Get detailed information for a specific video",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Video found successfully"
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
            description = "Video not found"
        )
    })
    public ResponseEntity<Video> getVideoById(
            @Parameter(description = "Video ID")
            @PathVariable Long id) {

        logger.debug("Get video by ID request: {}", id);
        
        Optional<Video> video = videoService.findVideoById(id);
        
        if (video.isPresent()) {
            logger.debug("Video found: {}", id);
            return ResponseEntity.ok(video.get());
        } else {
            logger.warn("Video not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get video statistics.
     * 
     * @return ResponseEntity with video statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get video statistics",
        description = "Get comprehensive video statistics and analytics",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = VideoStatsResponse.class)
            )
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
    public ResponseEntity<VideoStatsResponse> getVideoStatistics() {
        logger.debug("Get video statistics request");
        
        try {
            VideoStatsResponse stats = videoService.getVideoStatistics();
            logger.debug("Video statistics generated successfully");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Failed to generate video statistics: {}", e.getMessage());
            throw e;
        }
    }
} 
package com.goldmediatech.videometadata.controller;

import com.goldmediatech.videometadata.config.CacheConfig;
import com.goldmediatech.videometadata.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for cache management operations.
 * 
 * This controller provides endpoints for cache management, statistics,
 * and health monitoring.
 */
@RestController
@RequestMapping("/cache")
@Tag(name = "Cache Management", description = "Cache management and monitoring endpoints")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheService cacheService;

    /**
     * Get cache health status.
     * 
     * @return ResponseEntity with cache health information
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get cache health",
        description = "Get cache health status and information",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache health information retrieved successfully"
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
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        logger.debug("Getting cache health status");
        
        Map<String, Object> health = cacheService.getCacheHealth();
        logger.debug("Cache health status: {}", health.get("healthy"));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get cache statistics.
     * 
     * @return ResponseEntity with cache statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Get cache statistics",
        description = "Get detailed cache statistics and metrics",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache statistics retrieved successfully"
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
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        logger.debug("Getting cache statistics");
        
        Map<String, Object> stats = cacheService.getCacheStatistics();
        logger.debug("Retrieved statistics for {} caches", stats.size());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear all caches.
     * 
     * @return ResponseEntity with operation result
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear all caches",
        description = "Clear all application caches (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "All caches cleared successfully"
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
    public ResponseEntity<String> clearAllCaches() {
        logger.info("Clearing all caches");
        
        cacheService.clearAllCaches();
        
        return ResponseEntity.ok("All caches cleared successfully");
    }

    /**
     * Clear specific cache.
     * 
     * @param cacheName the name of the cache to clear
     * @return ResponseEntity with operation result
     */
    @DeleteMapping("/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear specific cache",
        description = "Clear a specific cache by name (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache cleared successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid cache name"
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
    public ResponseEntity<String> clearCache(
            @Parameter(description = "Cache name to clear")
            @PathVariable String cacheName) {

        logger.info("Clearing cache: {}", cacheName);
        
        cacheService.clearCache(cacheName);
        
        return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
    }

    /**
     * Clear video statistics cache.
     * 
     * @return ResponseEntity with operation result
     */
    @DeleteMapping("/clear/video-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear video statistics cache",
        description = "Clear video statistics cache (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Video statistics cache cleared successfully"
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
    public ResponseEntity<String> clearVideoStatsCache() {
        logger.info("Clearing video statistics cache");
        
        cacheService.clearVideoStatsCache();
        
        return ResponseEntity.ok("Video statistics cache cleared successfully");
    }

    /**
     * Clear job statistics cache.
     * 
     * @return ResponseEntity with operation result
     */
    @DeleteMapping("/clear/job-stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Clear job statistics cache",
        description = "Clear job statistics cache (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Job statistics cache cleared successfully"
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
    public ResponseEntity<String> clearJobStatsCache() {
        logger.info("Clearing job statistics cache");
        
        cacheService.clearJobStatsCache();
        
        return ResponseEntity.ok("Job statistics cache cleared successfully");
    }

    /**
     * Warm up caches.
     * 
     * @return ResponseEntity with operation result
     */
    @PostMapping("/warmup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Warm up caches",
        description = "Pre-load frequently accessed data into caches (ADMIN only)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cache warm-up completed successfully"
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
    public ResponseEntity<String> warmUpCaches() {
        logger.info("Starting cache warm-up");
        
        cacheService.warmUpCaches();
        
        return ResponseEntity.ok("Cache warm-up completed successfully");
    }
} 
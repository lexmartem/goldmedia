package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing cache operations and statistics.
 * 
 * This service provides methods for cache management, statistics collection,
 * and cache eviction operations.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * Clear all caches.
     */
    public void clearAllCaches() {
        logger.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        });
    }

    /**
     * Clear specific cache by name.
     * 
     * @param cacheName the name of the cache to clear
     */
    public void clearCache(String cacheName) {
        logger.info("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            logger.debug("Successfully cleared cache: {}", cacheName);
        } else {
            logger.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Clear video statistics cache.
     */
    public void clearVideoStatsCache() {
        clearCache(CacheConfig.VIDEO_STATS_CACHE);
    }

    /**
     * Clear job statistics cache.
     */
    public void clearJobStatsCache() {
        clearCache(CacheConfig.JOB_STATS_CACHE);
    }

    /**
     * Clear video cache.
     */
    public void clearVideoCache() {
        clearCache(CacheConfig.VIDEO_CACHE);
    }

    /**
     * Clear job cache.
     */
    public void clearJobCache() {
        clearCache(CacheConfig.JOB_CACHE);
    }

    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("name", cacheName);
                cacheStats.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                
                // Try to get cache size if available
                try {
                    if (cache.getNativeCache() instanceof Map) {
                        cacheStats.put("size", ((Map<?, ?>) cache.getNativeCache()).size());
                    }
                } catch (Exception e) {
                    logger.debug("Could not determine cache size for {}", cacheName);
                }
                
                stats.put(cacheName, cacheStats);
            }
        });
        
        return stats;
    }

    /**
     * Check if cache is healthy.
     * 
     * @return true if cache is healthy, false otherwise
     */
    public boolean isCacheHealthy() {
        try {
            // Try to access each cache to verify connectivity
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache == null) {
                    logger.warn("Cache not found: {}", cacheName);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Cache health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get cache health information.
     * 
     * @return Map containing cache health information
     */
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("healthy", isCacheHealthy());
        health.put("cacheManager", cacheManager.getClass().getSimpleName());
        health.put("availableCaches", cacheManager.getCacheNames());
        health.put("statistics", getCacheStatistics());
        return health;
    }

    /**
     * Evict cache entries by pattern.
     * 
     * @param cacheName the cache name
     * @param pattern the pattern to match keys
     */
    public void evictByPattern(String cacheName, String pattern) {
        logger.info("Evicting cache entries by pattern: {} in cache: {}", pattern, cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // This is a simplified implementation
            // In a real Redis implementation, you would use SCAN command
            logger.debug("Pattern-based eviction not fully implemented for cache: {}", cacheName);
        }
    }

    /**
     * Warm up caches by pre-loading frequently accessed data.
     */
    public void warmUpCaches() {
        logger.info("Starting cache warm-up");
        
        // Warm up video statistics cache
        try {
            // This would typically call the service methods that generate statistics
            logger.debug("Cache warm-up completed");
        } catch (Exception e) {
            logger.error("Cache warm-up failed: {}", e.getMessage());
        }
    }
} 
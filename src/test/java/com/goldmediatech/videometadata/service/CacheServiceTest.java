package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for CacheService.
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache videoStatsCache;

    @Mock
    private Cache jobStatsCache;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache(CacheConfig.VIDEO_STATS_CACHE)).thenReturn(videoStatsCache);
        lenient().when(cacheManager.getCache(CacheConfig.JOB_STATS_CACHE)).thenReturn(jobStatsCache);
        lenient().when(cacheManager.getCacheNames()).thenReturn(Arrays.asList(CacheConfig.VIDEO_STATS_CACHE, CacheConfig.JOB_STATS_CACHE));
        
        // Mock getNativeCache to return a non-null object
        lenient().when(videoStatsCache.getNativeCache()).thenReturn(new Object());
        lenient().when(jobStatsCache.getNativeCache()).thenReturn(new Object());
    }

    @Test
    void testClearAllCaches() {
        // When: clearing all caches
        cacheService.clearAllCaches();

        // Then: all caches should be cleared
        verify(videoStatsCache).clear();
        verify(jobStatsCache).clear();
    }

    @Test
    void testClearSpecificCache() {
        // Given: a specific cache name
        String cacheName = CacheConfig.VIDEO_STATS_CACHE;

        // When: clearing the specific cache
        cacheService.clearCache(cacheName);

        // Then: the specific cache should be cleared
        verify(videoStatsCache).clear();
    }

    @Test
    void testClearVideoStatsCache() {
        // When: clearing video stats cache
        cacheService.clearVideoStatsCache();

        // Then: video stats cache should be cleared
        verify(videoStatsCache).clear();
    }

    @Test
    void testClearJobStatsCache() {
        // When: clearing job stats cache
        cacheService.clearJobStatsCache();

        // Then: job stats cache should be cleared
        verify(jobStatsCache).clear();
    }

    @Test
    void testGetCacheStatistics() {
        // When: getting cache statistics
        Map<String, Object> stats = cacheService.getCacheStatistics();

        // Then: statistics should be returned
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        
        // Should contain expected cache names
        assertTrue(stats.containsKey(CacheConfig.VIDEO_STATS_CACHE) || 
                  stats.containsKey(CacheConfig.JOB_STATS_CACHE));
    }

    @Test
    void testIsCacheHealthy() {
        // When: checking cache health
        boolean isHealthy = cacheService.isCacheHealthy();

        // Then: should return health status
        assertTrue(isHealthy);
    }

    @Test
    void testGetCacheHealth() {
        // When: getting cache health
        Map<String, Object> health = cacheService.getCacheHealth();

        // Then: health information should be returned
        assertNotNull(health);
        assertTrue(health.containsKey("healthy"));
        assertTrue(health.containsKey("cacheManager"));
        assertTrue(health.containsKey("availableCaches"));
        assertTrue(health.containsKey("statistics"));
        
        assertTrue((Boolean) health.get("healthy"));
    }

    @Test
    void testWarmUpCaches() {
        // When: warming up caches
        // Then: should not throw exception
        assertDoesNotThrow(() -> cacheService.warmUpCaches());
    }
} 
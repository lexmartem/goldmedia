package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for caching functionality using real Redis.
 * 
 * This test uses Testcontainers to spin up a real Redis instance
 * and tests the actual caching behavior.
 * 
 * Requirements:
 * - Docker must be running on your machine
 * - Testcontainers will automatically manage the Redis container
 */
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testRedisConnection() {
        // This test verifies that we're actually connected to Redis
        // and not using the fallback cache manager
        
        // When: checking cache manager type
        String cacheManagerClass = cacheManager.getClass().getSimpleName();
        
        // Then: should be using Redis cache manager, not fallback
        assertTrue(cacheManagerClass.contains("Redis") || cacheManagerClass.contains("RedisCacheManager"));
    }

    @Test
    void testCacheHealth() {
        // When: checking cache health
        boolean isHealthy = cacheService.isCacheHealthy();
        var health = cacheService.getCacheHealth();

        // Then: cache should be healthy
        assertTrue(isHealthy);
        assertNotNull(health);
        assertTrue(health.containsKey("healthy"));
        assertTrue(health.containsKey("cacheManager"));
        assertTrue(health.containsKey("availableCaches"));
    }

    @Test
    void testCacheStatistics() {
        // When: getting cache statistics
        var stats = cacheService.getCacheStatistics();

        // Then: statistics should be available
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
    }

    @Test
    void testCacheManagerConfiguration() {
        // Then: cache manager should be configured with expected caches
        assertNotNull(cacheManager);
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.VIDEO_STATS_CACHE));
        assertTrue(cacheManager.getCacheNames().contains(CacheConfig.JOB_STATS_CACHE));
    }

    @Test
    void testCacheClearOperations() {
        // Given: cache service is available
        assertNotNull(cacheService);
        
        // When: clearing caches
        // Then: should not throw exceptions
        assertDoesNotThrow(() -> cacheService.clearVideoStatsCache());
        assertDoesNotThrow(() -> cacheService.clearJobStatsCache());
        assertDoesNotThrow(() -> cacheService.clearAllCaches());
    }

    @Test
    void testCacheWarmUp() {
        // When: warming up caches
        // Then: should not throw exceptions
        assertDoesNotThrow(() -> cacheService.warmUpCaches());
    }

    @Test
    void testBasicCacheOperations() {
        // Given: get a cache
        var cache = cacheManager.getCache(CacheConfig.VIDEO_STATS_CACHE);
        assertNotNull(cache);
        
        // When: putting and getting a simple value
        cache.put("test-key", "test-value");
        var retrieved = cache.get("test-key");
        
        // Then: value should be retrieved correctly
        assertNotNull(retrieved);
        assertEquals("test-value", retrieved.get());
    }
} 
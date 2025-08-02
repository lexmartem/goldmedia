package com.goldmediatech.videometadata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;

/**
 * Cache configuration for the Video Metadata Service.
 * 
 * This configuration sets up Redis-based caching with custom TTL settings
 * and key generation strategies for optimal performance.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache names used throughout the application.
     */
    public static final String VIDEO_STATS_CACHE = "video-stats";
    public static final String JOB_STATS_CACHE = "job-stats";
    public static final String VIDEO_CACHE = "videos";
    public static final String JOB_CACHE = "jobs";

    /**
     * Cache TTL durations.
     */
    public static final Duration VIDEO_STATS_TTL = Duration.ofMinutes(10);
    public static final Duration JOB_STATS_TTL = Duration.ofMinutes(5);
    public static final Duration VIDEO_CACHE_TTL = Duration.ofHours(1);
    public static final Duration JOB_CACHE_TTL = Duration.ofMinutes(15);

    /**
     * Configure Jackson ObjectMapper for Redis serialization.
     * 
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Configure Redis cache manager with custom TTL settings.
     * Only created when Redis is available and not disabled.
     * 
     * @param connectionFactory Redis connection factory
     * @param objectMapper Jackson ObjectMapper for serialization
     * @return configured cache manager
     */
    @Bean
    @Primary
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Create a custom serializer with proper Jackson configuration
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(VIDEO_STATS_CACHE, 
                    defaultConfig.entryTtl(VIDEO_STATS_TTL))
                .withCacheConfiguration(JOB_STATS_CACHE, 
                    defaultConfig.entryTtl(JOB_STATS_TTL))
                .withCacheConfiguration(VIDEO_CACHE, 
                    defaultConfig.entryTtl(VIDEO_CACHE_TTL))
                .withCacheConfiguration(JOB_CACHE, 
                    defaultConfig.entryTtl(JOB_CACHE_TTL))
                .build();
    }

    /**
     * Fallback cache manager for when Redis is not available.
     * Used in tests and when Redis is disabled.
     * 
     * @return fallback cache manager
     */
    @Bean
    @ConditionalOnProperty(name = "spring.autoconfigure.exclude", havingValue = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
    public CacheManager fallbackCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(VIDEO_STATS_CACHE, JOB_STATS_CACHE, VIDEO_CACHE, JOB_CACHE));
        return cacheManager;
    }

    /**
     * Custom key generator for cache keys.
     * 
     * @return key generator bean
     */
    @Bean
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName()).append(".");
            keyBuilder.append(method.getName());
            
            if (params != null && params.length > 0) {
                keyBuilder.append(".");
                keyBuilder.append(Arrays.toString(params));
            }
            
            return keyBuilder.toString();
        };
    }

    /**
     * Key generator specifically for statistics methods.
     * 
     * @return statistics key generator
     */
    @Bean
    public KeyGenerator statsKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append("stats.");
            keyBuilder.append(method.getName());
            
            // Add timestamp for cache invalidation based on time
            long currentHour = System.currentTimeMillis() / (1000 * 60 * 60); // Hour-based
            keyBuilder.append(".hour-").append(currentHour);
            
            return keyBuilder.toString();
        };
    }
} 
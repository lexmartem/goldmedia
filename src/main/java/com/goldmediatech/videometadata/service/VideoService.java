package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.CacheConfig;
import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportResponse;
import com.goldmediatech.videometadata.dto.response.VideoStatsResponse;
import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.repository.VideoRepository;
import com.goldmediatech.videometadata.service.external.VideoApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Video service for handling video business logic.
 * 
 * This service provides functionality for video import, querying, and statistics
 * for the Video Metadata Service.
 */
@Service
@Transactional
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private List<VideoApiService> videoApiServices;

    /**
     * Import videos from external API.
     * 
     * @param importRequest the import request
     * @return ImportResponse with import results
     */
    @CacheEvict(value = {CacheConfig.VIDEO_STATS_CACHE, CacheConfig.VIDEO_CACHE}, allEntries = true)
    public ImportResponse importVideos(ImportRequest importRequest) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting video import for source: {}, video IDs: {}", 
                   importRequest.source(), importRequest.videoIds());

        VideoApiService apiService = getApiServiceForSource(importRequest.source());
        if (apiService == null) {
            throw new RuntimeException("No API service available for source: " + importRequest.source());
        }

        List<String> videoIds = importRequest.videoIds();
        List<String> existingVideoIds = videoRepository.findExistingVideoIds(videoIds);
        List<String> newVideoIds = videoIds.stream()
                .filter(id -> !existingVideoIds.contains(id))
                .collect(Collectors.toList());

        List<Video> importedVideos = new ArrayList<>();
        List<String> failedVideoIds = new ArrayList<>();
        List<String> skippedVideoIds = new ArrayList<>();

        // Process videos in batches
        int batchSize = importRequest.batchSize();
        for (int i = 0; i < newVideoIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, newVideoIds.size());
            List<String> batch = newVideoIds.subList(i, endIndex);
            
            List<Video> batchVideos = apiService.getVideoMetadataBatch(batch);
            
            for (int j = 0; j < batch.size(); j++) {
                String videoId = batch.get(j);
                Video video = batchVideos.get(j);
                
                if (video != null) {
                    try {
                        Video savedVideo = videoRepository.save(video);
                        importedVideos.add(savedVideo);
                        logger.debug("Successfully imported video: {}", videoId);
                    } catch (Exception e) {
                        logger.error("Failed to save video {}: {}", videoId, e.getMessage());
                        failedVideoIds.add(videoId);
                    }
                } else {
                    logger.warn("Failed to retrieve metadata for video: {}", videoId);
                    failedVideoIds.add(videoId);
                }
            }
        }

        // Add existing videos to skipped list
        skippedVideoIds.addAll(existingVideoIds);

        long processingTime = System.currentTimeMillis() - startTime;
        
        ImportResponse response = new ImportResponse(
                videoIds.size(),
                importedVideos.size(),
                failedVideoIds.size(),
                skippedVideoIds.size(),
                failedVideoIds,
                skippedVideoIds,
                importRequest.source().name(),
                null, // importTimestamp will be set by the compact constructor
                processingTime
        );

        logger.info("Video import completed - Total: {}, Successful: {}, Failed: {}, Skipped: {}, Time: {}ms",
                   videoIds.size(), importedVideos.size(), failedVideoIds.size(), skippedVideoIds.size(), processingTime);

        return response;
    }

    /**
     * Find videos with filters and pagination.
     * 
     * @param source video source filter
     * @param uploadDateFrom start date filter
     * @param uploadDateTo end date filter
     * @param minDuration minimum duration filter
     * @param maxDuration maximum duration filter
     * @param pageable pagination information
     * @return Page of videos matching criteria
     */
    public Page<Video> findVideos(VideoSource source, LocalDateTime uploadDateFrom, LocalDateTime uploadDateTo,
                                 Integer minDuration, Integer maxDuration, Pageable pageable) {
        logger.debug("Finding videos with filters - source: {}, dateFrom: {}, dateTo: {}, duration: {}-{}",
                   source, uploadDateFrom, uploadDateTo, minDuration, maxDuration);

        Specification<Video> spec = Specification.where(null);

        if (source != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("source"), source));
        }

        if (uploadDateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("uploadDate"), uploadDateFrom));
        }

        if (uploadDateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("uploadDate"), uploadDateTo));
        }

        if (minDuration != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("duration"), minDuration));
        }

        if (maxDuration != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("duration"), maxDuration));
        }

        return videoRepository.findAll(spec, pageable);
    }

    /**
     * Find video by ID.
     * 
     * @param id the video ID
     * @return Optional containing video if found
     */
    public Optional<Video> findVideoById(Long id) {
        return videoRepository.findById(id);
    }

    /**
     * Find video by external video ID.
     * 
     * @param videoId the external video ID
     * @return Optional containing video if found
     */
    public Optional<Video> findVideoByVideoId(String videoId) {
        return videoRepository.findByVideoId(videoId);
    }

    /**
     * Get video statistics.
     * 
     * @return VideoStatsResponse with comprehensive statistics
     */
    @Cacheable(value = CacheConfig.VIDEO_STATS_CACHE, keyGenerator = "statsKeyGenerator")
    public VideoStatsResponse getVideoStatistics() {
        logger.debug("Generating video statistics");

        long totalVideos = videoRepository.count();
        
        Map<String, Long> videosBySource = new HashMap<>();
        Map<String, Double> averageDurationBySource = new HashMap<>();
        
        for (VideoSource source : VideoSource.values()) {
            long count = videoRepository.countBySource(source);
            videosBySource.put(source.name(), count);
            
            Double avgDuration = videoRepository.getAverageDurationBySource(source);
            if (avgDuration != null) {
                averageDurationBySource.put(source.name(), avgDuration);
            }
        }
        
        Long totalDuration = videoRepository.getTotalDuration();
        if (totalDuration == null) {
            totalDuration = 0L;
        }

        VideoStatsResponse response = new VideoStatsResponse(
                totalVideos, videosBySource, averageDurationBySource, totalDuration, null
        );

        logger.debug("Generated statistics - Total videos: {}, Total duration: {}s", totalVideos, totalDuration);
        
        return response;
    }

    /**
     * Get API service for the specified source.
     * 
     * @param source the video source
     * @return VideoApiService for the source, or null if not available
     */
    private VideoApiService getApiServiceForSource(VideoSource source) {
        return videoApiServices.stream()
                .filter(service -> service.getSource() == source && service.isAvailable())
                .findFirst()
                .orElse(null);
    }
} 
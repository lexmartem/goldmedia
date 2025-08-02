package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportResponse;
import com.goldmediatech.videometadata.entity.ImportJob;
import com.goldmediatech.videometadata.entity.ImportStatus;
import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.repository.ImportJobRepository;
import com.goldmediatech.videometadata.repository.VideoRepository;
import com.goldmediatech.videometadata.service.external.VideoApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Asynchronous service for background video import processing.
 * 
 * This service handles video imports in the background using reactive programming
 * to improve performance and responsiveness.
 */
@Service
@Transactional
public class AsyncVideoImportService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncVideoImportService.class);

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private List<VideoApiService> videoApiServices;

    /**
     * Start an asynchronous video import job.
     * 
     * @param importRequest the import request
     * @return CompletableFuture containing the import job
     */
    @Async("videoImportExecutor")
    public CompletableFuture<ImportJob> startAsyncImport(ImportRequest importRequest) {
        String jobId = generateJobId();
        logger.info("Starting async video import job: {}", jobId);

        // Create and save the job
        ImportJob job = new ImportJob(jobId, importRequest);
        job.setStatus(ImportStatus.RUNNING);
        job = importJobRepository.save(job);

        // Process the import reactively
        return processVideosReactive(importRequest, job)
            .subscribeOn(Schedulers.boundedElastic())
            .toFuture();
    }

    /**
     * Start an asynchronous video import job with a specific job ID.
     * 
     * @param importRequest the import request
     * @param jobId the job ID to use
     * @return CompletableFuture containing the import job
     */
    @Async("videoImportExecutor")
    public CompletableFuture<ImportJob> startAsyncImport(ImportRequest importRequest, String jobId) {
        logger.info("Starting async video import job: {}", jobId);

        // Create and save the job
        ImportJob job = new ImportJob(jobId, importRequest);
        job.setStatus(ImportStatus.RUNNING);
        job = importJobRepository.save(job);

        // Process the import reactively
        return processVideosReactive(importRequest, job)
            .subscribeOn(Schedulers.boundedElastic())
            .toFuture();
    }

    /**
     * Process videos reactively in the background.
     * 
     * @param importRequest the import request
     * @param job the import job
     * @return Mono containing the updated job
     */
    private Mono<ImportJob> processVideosReactive(ImportRequest importRequest, ImportJob job) {
        long startTime = System.currentTimeMillis();
        logger.info("Processing videos reactively for job: {}", job.getJobId());

        VideoApiService apiService = getApiServiceForSource(importRequest.source());
        if (apiService == null) {
            return Mono.error(new RuntimeException("No API service available for source: " + importRequest.source()));
        }

        List<String> videoIds = importRequest.videoIds();
        
        return Flux.fromIterable(videoIds)
            .buffer(importRequest.batchSize())
            .flatMap(batch -> processBatchReactive(batch, apiService), 2) // Process 2 batches concurrently
            .collectList()
            .map(results -> {
                // Aggregate results
                List<Video> importedVideos = new ArrayList<>();
                List<String> failedVideoIds = new ArrayList<>();
                List<String> skippedVideoIds = new ArrayList<>();

                for (BatchResult result : results) {
                    importedVideos.addAll(result.importedVideos());
                    failedVideoIds.addAll(result.failedVideoIds());
                    skippedVideoIds.addAll(result.skippedVideoIds());
                }

                // Create import response
                long processingTime = System.currentTimeMillis() - startTime;
                ImportResponse importResponse = new ImportResponse(
                    videoIds.size(),
                    importedVideos.size(),
                    failedVideoIds.size(),
                    skippedVideoIds.size(),
                    failedVideoIds,
                    skippedVideoIds,
                    importRequest.source().name(),
                    null,
                    processingTime
                );

                // Update job with results
                job.setStatus(ImportStatus.COMPLETED);
                job.setResult(importResponse);
                job.setCompletedAt(LocalDateTime.now());

                logger.info("Async import completed for job: {} - Total: {}, Successful: {}, Failed: {}, Skipped: {}",
                    job.getJobId(), videoIds.size(), importedVideos.size(), failedVideoIds.size(), skippedVideoIds.size());

                return importJobRepository.save(job);
            })
            .onErrorResume(error -> {
                logger.error("Async import failed for job: {}", job.getJobId(), error);
                job.setStatus(ImportStatus.FAILED);
                job.setErrorMessage(error.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                return Mono.just(importJobRepository.save(job));
            });
    }

    /**
     * Process a batch of videos reactively.
     * 
     * @param videoIds the video IDs to process
     * @param apiService the API service to use
     * @return Mono containing the batch result
     */
    private Mono<BatchResult> processBatchReactive(List<String> videoIds, VideoApiService apiService) {
        return Mono.fromCallable(() -> {
            // Check for existing videos
            List<String> existingVideoIds = videoRepository.findExistingVideoIds(videoIds);
            List<String> newVideoIds = videoIds.stream()
                .filter(id -> !existingVideoIds.contains(id))
                .collect(Collectors.toList());

            List<Video> importedVideos = new ArrayList<>();
            List<String> failedVideoIds = new ArrayList<>();
            List<String> skippedVideoIds = new ArrayList<>();

            // Process new videos
            if (!newVideoIds.isEmpty()) {
                List<Video> batchVideos = apiService.getVideoMetadataBatch(newVideoIds);
                
                for (int i = 0; i < newVideoIds.size(); i++) {
                    String videoId = newVideoIds.get(i);
                    Video video = batchVideos.get(i);
                    
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

            return new BatchResult(importedVideos, failedVideoIds, skippedVideoIds);
        }).subscribeOn(Schedulers.boundedElastic());
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

    /**
     * Generate a unique job ID.
     * 
     * @return unique job ID
     */
    private String generateJobId() {
        return "import-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Record class for batch processing results.
     */
    private record BatchResult(
        List<Video> importedVideos,
        List<String> failedVideoIds,
        List<String> skippedVideoIds
    ) {}
} 
package com.goldmediatech.videometadata.repository;

import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Video entity providing data access operations.
 * 
 * This repository extends JpaRepository and JpaSpecificationExecutor to provide
 * both basic CRUD operations and advanced querying capabilities.
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {

    /**
     * Find video by external video ID.
     * 
     * @param videoId the external video ID
     * @return Optional containing the video if found
     */
    Optional<Video> findByVideoId(String videoId);

    /**
     * Check if video exists by external video ID.
     * 
     * @param videoId the external video ID
     * @return true if video exists, false otherwise
     */
    boolean existsByVideoId(String videoId);

    /**
     * Find videos by source platform.
     * 
     * @param source the video source
     * @param pageable pagination information
     * @return Page of videos from the specified source
     */
    Page<Video> findBySource(VideoSource source, Pageable pageable);

    /**
     * Find videos by source and upload date range.
     * 
     * @param source the video source
     * @param uploadDateFrom start date (inclusive)
     * @param uploadDateTo end date (inclusive)
     * @param pageable pagination information
     * @return Page of videos matching the criteria
     */
    Page<Video> findBySourceAndUploadDateBetween(
            VideoSource source,
            LocalDateTime uploadDateFrom,
            LocalDateTime uploadDateTo,
            Pageable pageable);

    /**
     * Find videos by duration range.
     * 
     * @param minDuration minimum duration in seconds
     * @param maxDuration maximum duration in seconds
     * @param pageable pagination information
     * @return Page of videos within the duration range
     */
    Page<Video> findByDurationBetween(Integer minDuration, Integer maxDuration, Pageable pageable);

    /**
     * Find videos by source and duration range.
     * 
     * @param source the video source
     * @param minDuration minimum duration in seconds
     * @param maxDuration maximum duration in seconds
     * @param pageable pagination information
     * @return Page of videos matching the criteria
     */
    Page<Video> findBySourceAndDurationBetween(
            VideoSource source,
            Integer minDuration,
            Integer maxDuration,
            Pageable pageable);

    /**
     * Count videos by source.
     * 
     * @param source the video source
     * @return count of videos from the specified source
     */
    long countBySource(VideoSource source);

    /**
     * Get average duration by source.
     * 
     * @param source the video source
     * @return average duration in seconds, or null if no videos found
     */
    @Query("SELECT AVG(v.duration) FROM Video v WHERE v.source = :source")
    Double getAverageDurationBySource(@Param("source") VideoSource source);

    /**
     * Get total duration by source.
     * 
     * @param source the video source
     * @return total duration in seconds, or null if no videos found
     */
    @Query("SELECT SUM(v.duration) FROM Video v WHERE v.source = :source")
    Long getTotalDurationBySource(@Param("source") VideoSource source);

    /**
     * Get total duration across all sources.
     * 
     * @return total duration in seconds, or null if no videos found
     */
    @Query("SELECT SUM(v.duration) FROM Video v")
    Long getTotalDuration();

    /**
     * Find videos by multiple video IDs.
     * 
     * @param videoIds list of video IDs
     * @return list of videos matching the provided IDs
     */
    List<Video> findByVideoIdIn(List<String> videoIds);

    /**
     * Find videos that don't exist in the provided video IDs.
     * 
     * @param videoIds list of video IDs to check against
     * @return list of video IDs that don't exist in the database
     */
    @Query("SELECT v.videoId FROM Video v WHERE v.videoId IN :videoIds")
    List<String> findExistingVideoIds(@Param("videoIds") List<String> videoIds);
} 
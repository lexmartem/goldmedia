package com.goldmediatech.videometadata.repository;

import com.goldmediatech.videometadata.entity.ImportJob;
import com.goldmediatech.videometadata.entity.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing import job entities.
 */
@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    /**
     * Find import job by job ID.
     * 
     * @param jobId the job ID
     * @return Optional containing the import job if found
     */
    Optional<ImportJob> findByJobId(String jobId);

    /**
     * Find all import jobs by status.
     * 
     * @param status the import status
     * @return List of import jobs with the specified status
     */
    List<ImportJob> findByStatus(ImportStatus status);

    /**
     * Find all import jobs created after a specific date.
     * 
     * @param date the date to filter from
     * @return List of import jobs created after the date
     */
    List<ImportJob> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find all running jobs that started before a specific date (for cleanup).
     * 
     * @param date the date to filter from
     * @return List of running jobs started before the date
     */
    List<ImportJob> findByStatusAndStartedAtBefore(ImportStatus status, LocalDateTime date);

    /**
     * Count jobs by status.
     * 
     * @param status the import status
     * @return count of jobs with the specified status
     */
    long countByStatus(ImportStatus status);

    /**
     * Find jobs that have been running for too long (stuck jobs).
     * 
     * @param threshold the time threshold
     * @return List of jobs that have been running longer than the threshold
     */
    @Query("SELECT j FROM ImportJob j WHERE j.status = 'RUNNING' AND j.startedAt < :threshold")
    List<ImportJob> findStuckJobs(@Param("threshold") LocalDateTime threshold);
} 
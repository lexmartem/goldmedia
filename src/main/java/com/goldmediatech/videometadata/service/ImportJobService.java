package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.entity.ImportJob;
import com.goldmediatech.videometadata.entity.ImportStatus;
import com.goldmediatech.videometadata.repository.ImportJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing import job lifecycle and operations.
 */
@Service
@Transactional
public class ImportJobService {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobService.class);
    
    // Configuration constants for scheduled tasks
    private static final int DAYS_TO_KEEP_JOBS = 30; // Keep completed jobs for 30 days
    private static final int HOURS_THRESHOLD_FOR_STUCK_JOBS = 2; // Consider jobs stuck after 2 hours

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private AsyncVideoImportService asyncVideoImportService;

    /**
     * Create a new import job and start async processing.
     * 
     * @param importRequest the import request
     * @return the created import job
     */
    public ImportJob createAndStartJob(ImportRequest importRequest) {
        logger.info("Creating new import job for source: {}", importRequest.source());
        
        // Start the async import
        asyncVideoImportService.startAsyncImport(importRequest);
        
        // Return the job that was created by the async service
        // Note: In a real implementation, you might want to return the job ID immediately
        // and have the async service update the job status
        return null; // This will be handled differently in the controller
    }

    /**
     * Get import job by ID.
     * 
     * @param jobId the job ID
     * @return Optional containing the import job if found
     */
    public Optional<ImportJob> getJob(String jobId) {
        return importJobRepository.findByJobId(jobId);
    }

    /**
     * Get all import jobs.
     * 
     * @return List of all import jobs
     */
    public List<ImportJob> getAllJobs() {
        return (List<ImportJob>) importJobRepository.findAll();
    }

    /**
     * Get import jobs by status.
     * 
     * @param status the import status
     * @return List of import jobs with the specified status
     */
    public List<ImportJob> getJobsByStatus(ImportStatus status) {
        return importJobRepository.findByStatus(status);
    }

    /**
     * Get recent import jobs.
     * 
     * @param days number of days to look back
     * @return List of recent import jobs
     */
    public List<ImportJob> getRecentJobs(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return importJobRepository.findByCreatedAtAfter(cutoff);
    }

    /**
     * Cancel an import job.
     * 
     * @param jobId the job ID to cancel
     * @return true if job was cancelled, false if not found or already completed
     */
    public boolean cancelJob(String jobId) {
        Optional<ImportJob> jobOpt = importJobRepository.findByJobId(jobId);
        if (jobOpt.isPresent()) {
            ImportJob job = jobOpt.get();
            if (job.getStatus() == ImportStatus.PENDING || job.getStatus() == ImportStatus.RUNNING) {
                job.setStatus(ImportStatus.CANCELLED);
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage("Job cancelled by user");
                importJobRepository.save(job);
                logger.info("Cancelled import job: {}", jobId);
                return true;
            }
        }
        return false;
    }

    /**
     * Clean up old completed jobs.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(DAYS_TO_KEEP_JOBS);
        List<ImportJob> oldJobs = importJobRepository.findByStatusAndStartedAtBefore(
            ImportStatus.COMPLETED, cutoff);
        
        if (!oldJobs.isEmpty()) {
            importJobRepository.deleteAll(oldJobs);
            logger.info("Cleaned up {} old completed jobs", oldJobs.size());
        }
    }

    /**
     * Mark stuck jobs as failed.
     */
    @Scheduled(cron = "0 */30 * * * ?") // Run every 30 minutes
    public void markStuckJobsAsFailed() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(HOURS_THRESHOLD_FOR_STUCK_JOBS);
        List<ImportJob> stuckJobs = importJobRepository.findStuckJobs(threshold);
        
        for (ImportJob job : stuckJobs) {
            job.setStatus(ImportStatus.FAILED);
            job.setErrorMessage("Job marked as failed due to timeout");
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
            logger.warn("Marked stuck job as failed: {}", job.getJobId());
        }
        
        if (!stuckJobs.isEmpty()) {
            logger.info("Marked {} stuck jobs as failed", stuckJobs.size());
        }
    }

    /**
     * Get job statistics.
     * 
     * @return JobStatistics with counts by status
     */
    public JobStatistics getJobStatistics() {
        long totalJobs = importJobRepository.count();
        long pendingJobs = importJobRepository.countByStatus(ImportStatus.PENDING);
        long runningJobs = importJobRepository.countByStatus(ImportStatus.RUNNING);
        long completedJobs = importJobRepository.countByStatus(ImportStatus.COMPLETED);
        long failedJobs = importJobRepository.countByStatus(ImportStatus.FAILED);
        long cancelledJobs = importJobRepository.countByStatus(ImportStatus.CANCELLED);
        
        return new JobStatistics(totalJobs, pendingJobs, runningJobs, completedJobs, failedJobs, cancelledJobs);
    }

    /**
     * Record class for job statistics.
     */
    public record JobStatistics(
        long totalJobs,
        long pendingJobs,
        long runningJobs,
        long completedJobs,
        long failedJobs,
        long cancelledJobs
    ) {}
} 
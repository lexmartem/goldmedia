package com.goldmediatech.videometadata.entity;

/**
 * Enum representing the status of an import job.
 */
public enum ImportStatus {
    PENDING,    // Job is created but not yet started
    RUNNING,    // Job is currently being processed
    COMPLETED,  // Job completed successfully
    FAILED,     // Job failed with an error
    CANCELLED   // Job was cancelled
} 
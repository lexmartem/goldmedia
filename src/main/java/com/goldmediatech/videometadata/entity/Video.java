package com.goldmediatech.videometadata.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing video metadata.
 * 
 * This entity stores comprehensive video information including title, description,
 * duration, and metadata from mock video platforms.
 */
@Entity
@Table(name = "videos", indexes = {
    @Index(name = "idx_video_id", columnList = "videoId"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_upload_date", columnList = "uploadDate"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Video ID is required")
    @Column(name = "video_id", nullable = false, unique = true, length = 100)
    private String videoId;

    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Source is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private VideoSource source;

    @Min(value = 0, message = "Duration must be non-negative")
    @Column(name = "duration")
    private Integer duration; // in seconds

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "embed_url", length = 1000)
    private String embedUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // Default constructor
    public Video() {}

    // Constructor with required fields
    public Video(String videoId, String title, VideoSource source) {
        this.videoId = videoId;
        this.title = title;
        this.source = source;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VideoSource getSource() {
        return source;
    }

    public void setSource(VideoSource source) {
        this.source = source;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", source=" + source +
                ", duration=" + duration +
                ", uploadDate=" + uploadDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video video = (Video) o;
        return videoId != null && videoId.equals(video.getVideoId());
    }

    @Override
    public int hashCode() {
        return videoId != null ? videoId.hashCode() : 0;
    }
} 
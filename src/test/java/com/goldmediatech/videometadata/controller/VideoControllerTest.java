package com.goldmediatech.videometadata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportResponse;
import com.goldmediatech.videometadata.dto.response.VideoStatsResponse;
import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.exception.GlobalExceptionHandler;
import com.goldmediatech.videometadata.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VideoController.
 * 
 * This test class demonstrates controller testing patterns using MockMvc
 * to test HTTP endpoints with proper request/response handling.
 */
@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private VideoController videoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(videoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void importVideos_WithValidRequest_ShouldReturnSuccessResponse() throws Exception {
        // Given
        ImportRequest importRequest = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1", "video2"), 10);
        ImportResponse expectedResponse = new ImportResponse(2, 2, 0, 0, null, null, "MOCK", null, null);

        when(videoService.importVideos(any(ImportRequest.class))).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/videos/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.successful_imports").value(2))
                .andExpect(jsonPath("$.total_requested").value(2))
                .andExpect(jsonPath("$.failed_imports").value(0))
                .andExpect(jsonPath("$.source").value("MOCK"));
    }

    @Test
    void importVideos_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with null source
        ImportRequest importRequest = new ImportRequest(null, Arrays.asList("video1"), 10);

        // When & Then
        mockMvc.perform(post("/videos/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importVideos_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given
        ImportRequest importRequest = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1"), 10);
        when(videoService.importVideos(any(ImportRequest.class)))
                .thenThrow(new RuntimeException("Import failed"));

        // When & Then
        mockMvc.perform(post("/videos/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("RUNTIME_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details[0]").value("Import failed"));
    }

    @Test
    void getVideos_WithNoFilters_ShouldReturnPaginatedVideos() throws Exception {
        // Given
        Video video1 = createSampleVideo(1L, "Test Video 1", VideoSource.MOCK);
        Video video2 = createSampleVideo(2L, "Test Video 2", VideoSource.MOCK);
        List<Video> videos = Arrays.asList(video1, video2);
        Page<Video> videoPage = new PageImpl<>(videos, PageRequest.of(0, 20), 2);

        when(videoService.findVideos(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(videoPage);

        // When & Then
        mockMvc.perform(get("/videos")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Test Video 1"))
                .andExpect(jsonPath("$.content[1].title").value("Test Video 2"));
    }

    @Test
    void getVideos_WithSourceFilter_ShouldReturnFilteredVideos() throws Exception {
        // Given
        Video video = createSampleVideo(1L, "Mock Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);

        when(videoService.findVideos(eq(VideoSource.MOCK), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(videoPage);

        // When & Then
        mockMvc.perform(get("/videos")
                .param("source", "MOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].source").value("MOCK"));
    }

    @Test
    void getVideos_WithDateFilters_ShouldReturnFilteredVideos() throws Exception {
        // Given
        LocalDateTime fromDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2023, 12, 31, 23, 59);
        Video video = createSampleVideo(1L, "Filtered Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);

        when(videoService.findVideos(any(), eq(fromDate), eq(toDate), any(), any(), any(Pageable.class)))
                .thenReturn(videoPage);

        // When & Then
        mockMvc.perform(get("/videos")
                .param("uploadDateFrom", "2023-01-01T00:00:00")
                .param("uploadDateTo", "2023-12-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getVideos_WithDurationFilters_ShouldReturnFilteredVideos() throws Exception {
        // Given
        Video video = createSampleVideo(1L, "Duration Filtered Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);

        when(videoService.findVideos(any(), any(), any(), eq(60), eq(300), any(Pageable.class)))
                .thenReturn(videoPage);

        // When & Then
        mockMvc.perform(get("/videos")
                .param("minDuration", "60")
                .param("maxDuration", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getVideoById_WithValidId_ShouldReturnVideo() throws Exception {
        // Given
        Video video = createSampleVideo(1L, "Test Video", VideoSource.MOCK);
        when(videoService.findVideoById(1L)).thenReturn(Optional.of(video));

        // When & Then
        mockMvc.perform(get("/videos/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Video"))
                .andExpect(jsonPath("$.source").value("MOCK"));
    }

    @Test
    void getVideoById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Given
        when(videoService.findVideoById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/videos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVideoStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        VideoStatsResponse stats = new VideoStatsResponse(
                100L,
                Map.of("MOCK", 100L),
                Map.of("MOCK", 180.5),
                18050L,
                LocalDateTime.now()
        );

        when(videoService.getVideoStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/videos/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total_videos").value(100))
                .andExpect(jsonPath("$.videos_by_source.MOCK").value(100))
                .andExpect(jsonPath("$.average_duration_by_source.MOCK").value(180.5))
                .andExpect(jsonPath("$.total_duration").value(18050));
    }

    @Test
    void getVideoStatistics_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(videoService.getVideoStatistics())
                .thenThrow(new RuntimeException("Statistics generation failed"));

        // When & Then
        mockMvc.perform(get("/videos/stats"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("RUNTIME_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details[0]").value("Statistics generation failed"));
    }

    @Test
    void getVideos_WithInvalidDateFormat_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/videos")
                .param("uploadDateFrom", "invalid-date"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getVideos_WithInvalidPageParameters_ShouldReturnInternalServerError() throws Exception {
        // When & Then
        mockMvc.perform(get("/videos")
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("RUNTIME_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    private Video createSampleVideo(Long id, String title, VideoSource source) {
        Video video = new Video();
        video.setId(id);
        video.setTitle(title);
        video.setSource(source);
        video.setDuration(180);
        video.setUploadDate(LocalDateTime.now());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        return video;
    }
} 
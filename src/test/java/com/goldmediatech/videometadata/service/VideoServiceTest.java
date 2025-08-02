package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.dto.response.ImportResponse;
import com.goldmediatech.videometadata.dto.response.VideoStatsResponse;
import com.goldmediatech.videometadata.entity.Video;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.repository.VideoRepository;
import com.goldmediatech.videometadata.service.external.VideoApiService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VideoService.
 * 
 * This test class provides comprehensive coverage for video import,
 * querying, and statistics functionality.
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoService videoService;

    private VideoApiService mockApiService;

    @BeforeEach
    void setUp() {
        // Create mock API service
        mockApiService = mock(VideoApiService.class);
        
        // Inject the mocked service into the VideoService
        ReflectionTestUtils.setField(videoService, "videoApiServices", 
            Arrays.asList(mockApiService));
    }

    @Test
    void importVideos_WithValidRequest_ShouldReturnSuccessResponse() {
        // Given
        ImportRequest request = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1", "video2"), 10);
        Video video1 = createSampleVideo("video1", "Test Video 1", VideoSource.MOCK);
        Video video2 = createSampleVideo("video2", "Test Video 2", VideoSource.MOCK);
        
        when(mockApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockApiService.isAvailable()).thenReturn(true);
        when(mockApiService.getVideoMetadataBatch(Arrays.asList("video1", "video2")))
                .thenReturn(Arrays.asList(video1, video2));
        when(videoRepository.findExistingVideoIds(Arrays.asList("video1", "video2")))
                .thenReturn(Collections.emptyList());
        when(videoRepository.save(any(Video.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResponse response = videoService.importVideos(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.totalRequested());
        assertEquals(2, response.successfulImports());
        assertEquals(0, response.failedImports());
        assertEquals(0, response.skippedImports());
        assertEquals("MOCK", response.source());
        assertTrue(response.processingTimeMs() >= 0);
        
        verify(videoRepository).findExistingVideoIds(Arrays.asList("video1", "video2"));
        verify(videoRepository, times(2)).save(any(Video.class));
    }

    @Test
    void importVideos_WithExistingVideos_ShouldSkipThem() {
        // Given
        ImportRequest request = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1", "video2"), 10);
        Video video1 = createSampleVideo("video1", "Test Video 1", VideoSource.MOCK);
        
        when(mockApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockApiService.isAvailable()).thenReturn(true);
        when(mockApiService.getVideoMetadataBatch(Arrays.asList("video2")))
                .thenReturn(Arrays.asList(video1));
        when(videoRepository.findExistingVideoIds(Arrays.asList("video1", "video2")))
                .thenReturn(Arrays.asList("video1"));
        when(videoRepository.save(any(Video.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResponse response = videoService.importVideos(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.totalRequested());
        assertEquals(1, response.successfulImports());
        assertEquals(0, response.failedImports());
        assertEquals(1, response.skippedImports());
        assertTrue(response.skippedVideoIds().contains("video1"));
    }

    @Test
    void importVideos_WithApiFailure_ShouldHandleErrors() {
        // Given
        ImportRequest request = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1", "video2"), 10);
        
        when(mockApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockApiService.isAvailable()).thenReturn(true);
        when(mockApiService.getVideoMetadataBatch(Arrays.asList("video1", "video2")))
                .thenReturn(Arrays.asList(null, null)); // Both videos failed
        when(videoRepository.findExistingVideoIds(Arrays.asList("video1", "video2")))
                .thenReturn(Collections.emptyList());

        // When
        ImportResponse response = videoService.importVideos(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.totalRequested());
        assertEquals(0, response.successfulImports());
        assertEquals(2, response.failedImports());
        assertEquals(0, response.skippedImports());
        assertTrue(response.failedVideoIds().contains("video1"));
        assertTrue(response.failedVideoIds().contains("video2"));
    }

    @Test
    void importVideos_WithNoApiService_ShouldThrowException() {
        // Given
        ImportRequest request = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1"), 10);
        
        when(mockApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockApiService.isAvailable()).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> videoService.importVideos(request));
        assertEquals("No API service available for source: Mock", exception.getMessage());
    }

    @Test
    void findVideos_WithNoFilters_ShouldReturnAllVideos() {
        // Given
        Video video1 = createSampleVideo("video1", "Test Video 1", VideoSource.MOCK);
        Video video2 = createSampleVideo("video2", "Test Video 2", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video1, video2), PageRequest.of(0, 20), 2);
        
        when(videoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(videoPage);

        // When
        Page<Video> result = videoService.findVideos(null, null, null, null, null, PageRequest.of(0, 20));

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        
        verify(videoRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 20)));
    }

    @Test
    void findVideos_WithSourceFilter_ShouldApplyFilter() {
        // Given
        Video video = createSampleVideo("video1", "Mock Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);
        
        when(videoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(videoPage);

        // When
        Page<Video> result = videoService.findVideos(VideoSource.MOCK, null, null, null, null, PageRequest.of(0, 20));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(VideoSource.MOCK, result.getContent().get(0).getSource());
    }

    @Test
    void findVideos_WithDateFilters_ShouldApplyFilters() {
        // Given
        LocalDateTime fromDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2023, 12, 31, 23, 59);
        Video video = createSampleVideo("video1", "Filtered Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);
        
        when(videoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(videoPage);

        // When
        Page<Video> result = videoService.findVideos(null, fromDate, toDate, null, null, PageRequest.of(0, 20));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findVideos_WithDurationFilters_ShouldApplyFilters() {
        // Given
        Video video = createSampleVideo("video1", "Duration Filtered Video", VideoSource.MOCK);
        Page<Video> videoPage = new PageImpl<>(Arrays.asList(video), PageRequest.of(0, 20), 1);
        
        when(videoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(videoPage);

        // When
        Page<Video> result = videoService.findVideos(null, null, null, 60, 300, PageRequest.of(0, 20));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findVideoById_WithValidId_ShouldReturnVideo() {
        // Given
        Video video = createSampleVideo("video1", "Test Video", VideoSource.MOCK);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        Optional<Video> result = videoService.findVideoById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("video1", result.get().getVideoId());
        assertEquals("Test Video", result.get().getTitle());
    }

    @Test
    void findVideoById_WithInvalidId_ShouldReturnEmpty() {
        // Given
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Video> result = videoService.findVideoById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findVideoByVideoId_WithValidId_ShouldReturnVideo() {
        // Given
        Video video = createSampleVideo("video1", "Test Video", VideoSource.MOCK);
        when(videoRepository.findByVideoId("video1")).thenReturn(Optional.of(video));

        // When
        Optional<Video> result = videoService.findVideoByVideoId("video1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("video1", result.get().getVideoId());
    }

    @Test
    void getVideoStatistics_ShouldReturnComprehensiveStats() {
        // Given
        when(videoRepository.count()).thenReturn(100L);
        when(videoRepository.countBySource(VideoSource.MOCK)).thenReturn(100L);
        when(videoRepository.getAverageDurationBySource(VideoSource.MOCK)).thenReturn(180.5);
        when(videoRepository.getTotalDuration()).thenReturn(18050L);

        // When
        VideoStatsResponse stats = videoService.getVideoStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(100L, stats.totalVideos());
        assertEquals(100L, stats.videosBySource().get("MOCK"));
        assertEquals(180.5, stats.averageDurationBySource().get("MOCK"));
        assertEquals(18050L, stats.totalDuration());
    }

    @Test
    void getVideoStatistics_WithNullValues_ShouldHandleGracefully() {
        // Given
        when(videoRepository.count()).thenReturn(0L);
        when(videoRepository.countBySource(VideoSource.MOCK)).thenReturn(0L);
        when(videoRepository.getAverageDurationBySource(VideoSource.MOCK)).thenReturn(null);
        when(videoRepository.getTotalDuration()).thenReturn(null);

        // When
        VideoStatsResponse stats = videoService.getVideoStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.totalVideos());
        assertEquals(0L, stats.totalDuration());
        // videosBySource will contain entries for all sources with count 0
        assertFalse(stats.videosBySource().isEmpty());
        assertTrue(stats.averageDurationBySource().isEmpty());
    }

    @Test
    void importVideos_WithBatchSize_ShouldProcessInBatches() {
        // Given
        ImportRequest request = new ImportRequest(VideoSource.MOCK, Arrays.asList("video1", "video2", "video3"), 2);
        
        Video video1 = createSampleVideo("video1", "Test Video 1", VideoSource.MOCK);
        Video video2 = createSampleVideo("video2", "Test Video 2", VideoSource.MOCK);
        Video video3 = createSampleVideo("video3", "Test Video 3", VideoSource.MOCK);
        
        when(mockApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockApiService.isAvailable()).thenReturn(true);
        when(mockApiService.getVideoMetadataBatch(Arrays.asList("video1", "video2")))
                .thenReturn(Arrays.asList(video1, video2));
        when(mockApiService.getVideoMetadataBatch(Arrays.asList("video3")))
                .thenReturn(Arrays.asList(video3));
        when(videoRepository.findExistingVideoIds(Arrays.asList("video1", "video2", "video3")))
                .thenReturn(Collections.emptyList());
        when(videoRepository.save(any(Video.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ImportResponse response = videoService.importVideos(request);

        // Then
        assertNotNull(response);
        assertEquals(3, response.totalRequested());
        assertEquals(3, response.successfulImports());
        
        verify(mockApiService).getVideoMetadataBatch(Arrays.asList("video1", "video2"));
        verify(mockApiService).getVideoMetadataBatch(Arrays.asList("video3"));
    }

    private Video createSampleVideo(String videoId, String title, VideoSource source) {
        Video video = new Video(videoId, title, source);
        video.setDescription("Test description");
        video.setDuration(180);
        video.setUploadDate(LocalDateTime.now());
        video.setThumbnailUrl("https://example.com/thumbnail.jpg");
        video.setEmbedUrl("https://example.com/embed");
        video.setMetadata("{\"test\":\"data\"}");
        return video;
    }
} 
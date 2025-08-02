package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.dto.request.ImportRequest;
import com.goldmediatech.videometadata.entity.ImportJob;
import com.goldmediatech.videometadata.entity.ImportStatus;
import com.goldmediatech.videometadata.entity.VideoSource;
import com.goldmediatech.videometadata.repository.ImportJobRepository;
import com.goldmediatech.videometadata.repository.VideoRepository;
import com.goldmediatech.videometadata.service.external.MockVideoApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for AsyncVideoImportService.
 */
@ExtendWith(MockitoExtension.class)
class AsyncVideoImportServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private MockVideoApiService mockVideoApiService;

    @InjectMocks
    private AsyncVideoImportService asyncVideoImportService;

    private ImportRequest testImportRequest;

    @BeforeEach
    void setUp() {
        testImportRequest = new ImportRequest(
            VideoSource.MOCK,
            Arrays.asList("video1", "video2", "video3"),
            2
        );
        
        // Mock the videoApiServices list
        ReflectionTestUtils.setField(asyncVideoImportService, "videoApiServices", Arrays.asList(mockVideoApiService));
    }

    @Test
    void testStartAsyncImport_CreatesJobAndStartsProcessing() {
        // Arrange
        String jobId = "test-job-id";
        ImportJob mockJob = new ImportJob(jobId, testImportRequest);
        mockJob.setStatus(ImportStatus.RUNNING);
        
        when(importJobRepository.save(any(ImportJob.class))).thenReturn(mockJob);
        when(mockVideoApiService.getSource()).thenReturn(VideoSource.MOCK);
        when(mockVideoApiService.isAvailable()).thenReturn(true);
        when(mockVideoApiService.getVideoMetadataBatch(any())).thenReturn(Arrays.asList(null, null, null));

        // Act
        CompletableFuture<ImportJob> future = asyncVideoImportService.startAsyncImport(testImportRequest, jobId);

        // Assert
        assertNotNull(future);
        verify(importJobRepository, times(1)).save(any(ImportJob.class));
    }

    @Test
    void testGenerateJobId_ReturnsUniqueId() {
        // Act
        String jobId1 = (String) ReflectionTestUtils.invokeMethod(asyncVideoImportService, "generateJobId");
        String jobId2 = (String) ReflectionTestUtils.invokeMethod(asyncVideoImportService, "generateJobId");

        // Assert
        assertNotNull(jobId1);
        assertNotNull(jobId2);
        assertNotEquals(jobId1, jobId2);
        assertTrue(jobId1.startsWith("import-"));
        assertTrue(jobId2.startsWith("import-"));
    }
} 
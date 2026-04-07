package com.briefflow.unit.service;

import com.briefflow.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(tempDir.toString());
        fileStorageService.init();
    }

    @Test
    void should_storeFile_when_validFile() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "fake-image-bytes".getBytes()
        );

        String url = fileStorageService.store(file, "logos", "42.png");

        assertEquals("/uploads/logos/42.png", url);
        Path storedFile = tempDir.resolve("logos").resolve("42.png");
        assertTrue(Files.exists(storedFile));
    }

    @Test
    void should_overwriteExisting_when_sameFilename() throws IOException {
        MultipartFile first = new MockMultipartFile(
                "file", "logo.png", "image/png", "first-content".getBytes()
        );
        MultipartFile second = new MockMultipartFile(
                "file", "logo.png", "image/png", "second-content".getBytes()
        );

        fileStorageService.store(first, "logos", "1.png");
        fileStorageService.store(second, "logos", "1.png");

        Path storedFile = tempDir.resolve("logos").resolve("1.png");
        assertTrue(Files.exists(storedFile));
        assertEquals("second-content", new String(Files.readAllBytes(storedFile)));
    }

    @Test
    void should_deleteFile_when_exists() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "delete-me.png", "image/png", "content".getBytes()
        );
        fileStorageService.store(file, "logos", "delete-me.png");

        Path storedFile = tempDir.resolve("logos").resolve("delete-me.png");
        assertTrue(Files.exists(storedFile));

        fileStorageService.delete("/uploads/logos/delete-me.png");

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void should_notThrow_when_deletingNonExistentFile() {
        assertDoesNotThrow(() -> fileStorageService.delete("/uploads/logos/nonexistent.png"));
    }

    @Test
    void should_notThrow_when_deletingNullUrl() {
        assertDoesNotThrow(() -> fileStorageService.delete(null));
    }

    @Test
    void should_notThrow_when_deletingBlankUrl() {
        assertDoesNotThrow(() -> fileStorageService.delete("   "));
    }

    @Test
    void should_createSubdirectory_when_notExists() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "img.png", "image/png", "bytes".getBytes()
        );

        String url = fileStorageService.store(file, "new-subdir", "img.png");

        assertEquals("/uploads/new-subdir/img.png", url);
        assertTrue(Files.exists(tempDir.resolve("new-subdir").resolve("img.png")));
    }
}

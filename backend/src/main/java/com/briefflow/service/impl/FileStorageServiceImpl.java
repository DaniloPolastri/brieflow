package com.briefflow.service.impl;

import com.briefflow.exception.FileStorageException;
import com.briefflow.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadRoot;

    public FileStorageServiceImpl(
            @Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subdirectory, String filename) {
        try {
            Path targetDir = uploadRoot.resolve(subdirectory).normalize();
            validatePath(targetDir);
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename).normalize();
            validatePath(targetFile);

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subdirectory + "/" + filename;
        } catch (IOException e) {
            throw new FileStorageException("Could not store file " + filename, e);
        }
    }

    @Override
    public void delete(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return;
        }
        try {
            // Strip leading "/uploads/" prefix to get the relative path
            String relativePath = relativeUrl.replaceFirst("^/uploads/", "");
            Path targetFile = uploadRoot.resolve(relativePath).normalize();
            validatePath(targetFile);
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            throw new FileStorageException("Could not delete file: " + relativeUrl, e);
        }
    }

    @Override
    public Resource load(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            throw new FileStorageException("Empty file URL");
        }
        String relativePath = relativeUrl.replaceFirst("^/uploads/", "");
        Path targetFile = uploadRoot.resolve(relativePath).normalize();
        validatePath(targetFile);
        if (!Files.exists(targetFile)) {
            throw new FileStorageException("File not found: " + relativeUrl);
        }
        try {
            return new UrlResource(targetFile.toUri());
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not load file: " + relativeUrl, e);
        }
    }

    private void validatePath(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new FileStorageException("Path traversal attempt detected: " + path);
        }
    }
}

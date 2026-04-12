package com.briefflow.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String subdirectory, String filename);
    void delete(String relativeUrl);
    Resource load(String relativeUrl);
}

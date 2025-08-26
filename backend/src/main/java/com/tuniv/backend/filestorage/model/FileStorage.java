package com.tuniv.backend.filestorage.model;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String storeFile(MultipartFile file);
}

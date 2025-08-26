package com.tuniv.backend.filestorage.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.filestorage.model.FileStorage;
import com.tuniv.backend.filestorage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    // --- FIX: Inject the interface, not a concrete class ---
    private final FileStorage fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }
        try {
            // --- FIX: Call the simpler interface method ---
            String fileUrl = fileStorageService.storeFile(file);
            
            // Return a JSON object with the URL
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            // Catch a more generic Exception as the service might throw a RuntimeException
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
}
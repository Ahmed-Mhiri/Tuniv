package com.tuniv.backend.filestorage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tuniv.backend.filestorage.model.FileStorage;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileStorageService implements FileStorage {

    private final Path rootStorageLocation;

    public LocalFileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the root upload directory.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            Path targetDirectory = this.rootStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetDirectory);

            String fileExtension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFilename.substring(i);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = targetDirectory.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(subDirectory + "/")
                    .path(uniqueFileName)
                    .toUriString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }

    // ✨ --- NEW: DELETE FILE METHOD --- ✨
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            // Example URL: http://localhost:8080/uploads/questions/abc.jpg
            // We need to extract the path part: /uploads/questions/abc.jpg
            String pathPart = new java.net.URL(fileUrl).getPath();
            
            // Assuming your context path is "/" and your uploads are served from "/uploads/**"
            // we need to remove the leading "/uploads/" to get the relative path
            // For robustness, find the position of "/uploads/"
            int uploadsIndex = pathPart.indexOf("/uploads/");
            if (uploadsIndex == -1) {
                // Or log a warning if the URL format is unexpected
                return; 
            }
            
            String relativePath = pathPart.substring(uploadsIndex + "/uploads/".length());
            Path filePath = this.rootStorageLocation.resolve(relativePath).normalize();
            
            // Security check: ensure the resolved path is still within the storage directory
            if (!filePath.startsWith(this.rootStorageLocation)) {
                throw new SecurityException("Cannot delete file outside of the storage directory.");
            }

            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            // Log the exception, but don't re-throw as a fatal error
            System.err.println("Error deleting local file: " + fileUrl + " - " + e.getMessage());
        }
    }
}
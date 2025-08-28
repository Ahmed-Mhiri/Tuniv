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
            // --- FIX: Create the target subdirectory (e.g., /uploads/questions) ---
            Path targetDirectory = this.rootStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetDirectory);

            String fileExtension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFilename.substring(i);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Resolve the final path inside the subdirectory
            Path targetLocation = targetDirectory.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Return the web-accessible URL, including the subdirectory
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(subDirectory + "/")
                    .path(uniqueFileName)
                    .toUriString();

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }
}
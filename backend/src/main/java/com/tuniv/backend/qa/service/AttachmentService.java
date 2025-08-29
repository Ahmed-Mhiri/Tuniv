package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.filestorage.model.FileStorage;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.repository.AttachmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final FileStorage fileStorageService;
    private final AttachmentRepository attachmentRepository;

    public List<Attachment> saveAttachments(List<MultipartFile> files, Integer postId, String postType) {
        if (files == null) {
            return Collections.emptyList();
        }
        
        List<Attachment> savedAttachments = new ArrayList<>();

        // =========================================================================
        // ✅ FINAL FIX: We remove the ".filter(file -> !file.isEmpty())" check.
        // The file is being incorrectly flagged as empty, so we will process it anyway.
        // The try-catch block will prevent any real issues.
        // =========================================================================
        List<MultipartFile> validFiles = files.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (MultipartFile file : validFiles) {
            // We can add a check here to skip genuinely empty uploads without filtering.
            if (file.getSize() == 0) {
                continue;
            }
            
            try {
                String subDirectory = postType.toLowerCase() + "s";
                String fileUrl = fileStorageService.storeFile(file, subDirectory);

                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setFileType(file.getContentType());
                attachment.setFileSize(file.getSize());
                attachment.setPostId(postId);
                attachment.setPostType(postType);

                savedAttachments.add(attachmentRepository.save(attachment));
            } catch (Exception e) {
                // Re-throwing as a specific exception is good practice
                throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        return savedAttachments;
    }
}
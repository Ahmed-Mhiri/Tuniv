package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.filestorage.model.FileStorage;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.repository.AttachmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j // Using Slf4j for better logging
public class AttachmentService {

    private final FileStorage fileStorageService; // Use the interface
    private final AttachmentRepository attachmentRepository;

    /**
     * Saves a list of uploaded files and associates them with a given Post.
     */
    @Transactional
    public List<Attachment> saveAttachments(List<MultipartFile> files, Post post) {
        if (files == null || files.isEmpty() || post == null || post.getId() == null) {
            return Collections.emptyList();
        }

        List<Attachment> savedAttachments = new ArrayList<>();

        List<MultipartFile> validFiles = files.stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getSize() > 0)
                .collect(Collectors.toList());

        for (MultipartFile file : validFiles) {
            try {
                // Determine subdirectory from the Post's class name (e.g., "Question" -> "questions")
                String postType = post.getClass().getSimpleName().toLowerCase();
                String subDirectory = postType + "s";
                String fileUrl = fileStorageService.storeFile(file, subDirectory);

                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setFileType(file.getContentType());
                attachment.setFileSize(file.getSize());
                
                // ✨ FIX: Use the helper method from the Post entity.
                // This correctly sets both sides of the relationship in memory,
                // ensuring the Post object's attachment collection is aware of the new file.
                post.addAttachment(attachment);

                savedAttachments.add(attachmentRepository.save(attachment));
            } catch (Exception e) {
                log.error("Failed to store file: {}", file.getOriginalFilename(), e);
                // Depending on requirements, you might want to stop or continue
                throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        return savedAttachments;
    }

    /**
     * Deletes a set of attachments. This involves deleting the physical file
     * from storage and letting JPA handle the database record removal via orphanRemoval.
     */
    public void deleteAttachments(Set<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        for (Attachment attachment : attachments) {
            try {
                // Delete the physical file using its URL
                fileStorageService.deleteFile(attachment.getFileUrl());
            } catch (Exception e) {
                // Log the error but don't rethrow. This ensures that even if one file
                // fails to delete from storage, the process continues, and the database
                // records can still be cleaned up.
                log.error("Failed to delete file from storage: {}. Error: {}", attachment.getFileUrl(), e.getMessage());
            }
        }
        // The database records will be deleted by JPA when the parent Post's
        // attachment collection is modified and saved, thanks to `orphanRemoval=true`.
    }
}



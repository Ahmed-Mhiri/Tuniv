package com.tuniv.backend.qa.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

@Slf4j
@Service
@RequiredArgsConstructor
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

        List<MultipartFile> validFiles = files.stream()
            .filter(file -> file != null && !file.isEmpty() && isAllowedFileType(file))
            .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            return Collections.emptyList();
        }

        validateTotalFileSize(validFiles);

        List<Attachment> newAttachments = new ArrayList<>();
        String subDirectory = post.getPostType().toLowerCase() + "s";

        for (MultipartFile file : validFiles) {
            try {
                String fileUrl = fileStorageService.storeFile(file, subDirectory);
                
                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setMimeType(file.getContentType());
                attachment.setFileType(file.getContentType()); // You might want to derive a simpler type
                attachment.setFileSize(file.getSize());
                
                // ✅ ARCHITECTURAL FIX: Set the relationship directly on the attachment.
                attachment.setPost(post);
                attachment.setUploadedBy(post.getAuthor());

                // ✅ REMOVED: post.addAttachment(attachment); - This is no longer valid.
                
                newAttachments.add(attachment);
            } catch (Exception e) {
                log.error("Failed to store file: {}", file.getOriginalFilename(), e);
                // Rollback transaction by rethrowing
                throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        
        // ✅ OPTIMIZED: Save all new attachments in a single batch query.
        List<Attachment> savedAttachments = attachmentRepository.saveAll(newAttachments);
        log.info("Saved {} attachments for post ID: {}", savedAttachments.size(), post.getId());
        return savedAttachments;
    }

    /**
     * Deletes a set of attachments. This involves deleting the physical file
     * from storage and letting JPA handle the database record removal via orphanRemoval.
     */
    @Transactional
    public void deleteAttachments(Set<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        List<String> fileUrls = new ArrayList<>();
        List<Integer> attachmentIds = new ArrayList<>();

        for (Attachment attachment : attachments) {
            try {
                fileUrls.add(attachment.getFileUrl());
                attachmentIds.add(attachment.getAttachmentId());
            } catch (Exception e) {
                log.error("Error preparing attachment for deletion: {}", attachment.getAttachmentId(), e);
            }
        }

        // ✅ OPTIMIZED: Batch delete files from storage
        batchDeleteFiles(fileUrls);

        // ✅ OPTIMIZED: Batch delete database records
        if (!attachmentIds.isEmpty()) {
            batchDeleteAttachments(attachmentIds);
        }

        log.info("Deleted {} attachments from storage and database", attachments.size());
    }

    /**
     * ✅ NEW: Batch delete attachments by post ID
     */
    @Transactional
    public void deleteAttachmentsByPostId(Integer postId) {
        if (postId == null) {
            return;
        }

        try {
            // Get all attachments for the post
            List<Attachment> attachments = attachmentRepository.findByPostIdIn(List.of(postId));
            
            if (!attachments.isEmpty()) {
                deleteAttachments(new HashSet<>(attachments));
                log.info("Deleted all {} attachments for post ID: {}", attachments.size(), postId);
            }
        } catch (Exception e) {
            log.error("Failed to delete attachments for post ID: {}", postId, e);
            throw new RuntimeException("Failed to delete attachments for post: " + postId, e);
        }
    }

    /**
     * ✅ NEW: Get attachment with post details
     */
    @Transactional(readOnly = true)
    public Optional<Attachment> getAttachmentWithDetails(Integer attachmentId) {
        return attachmentRepository.findWithPostAndAuthorById(attachmentId);
    }

    /**
     * ✅ NEW: Get storage usage statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Long totalStorageUsed = attachmentRepository.getTotalStorageUsed();
            Long totalAttachments = attachmentRepository.count();
            List<Object[]> fileTypeStats = attachmentRepository.countAttachmentsByFileType();
            
            stats.put("totalStorageUsed", totalStorageUsed != null ? totalStorageUsed : 0);
            stats.put("totalAttachments", totalAttachments);
            stats.put("fileTypeDistribution", convertFileTypeStats(fileTypeStats));
            
        } catch (Exception e) {
            log.error("Error fetching storage statistics", e);
            stats.put("error", "Unable to fetch storage statistics");
        }
        
        return stats;
    }

    /**
     * ✅ NEW: Get storage used by specific user
     */
    @Transactional(readOnly = true)
    public Long getStorageUsedByUser(Integer userId) {
        try {
            Long storageUsed = attachmentRepository.getStorageUsedByUser(userId);
            return storageUsed != null ? storageUsed : 0L;
        } catch (Exception e) {
            log.error("Error fetching storage used by user ID: {}", userId, e);
            return 0L;
        }
    }

    /**
     * ✅ NEW: Clean up orphaned attachments (attachments without posts)
     */
    @Transactional
    public int cleanupOrphanedAttachments() {
        try {
            List<Attachment> orphanedAttachments = attachmentRepository.findOrphanedAttachments();
            
            if (!orphanedAttachments.isEmpty()) {
                log.info("Found {} orphaned attachments to clean up", orphanedAttachments.size());
                deleteAttachments(new HashSet<>(orphanedAttachments));
                return orphanedAttachments.size();
            }
            
            return 0;
        } catch (Exception e) {
            log.error("Error during orphaned attachments cleanup", e);
            return 0;
        }
    }

    /**
     * ✅ NEW: Validate file type
     */
    private boolean isAllowedFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        // Allowed file types
        Set<String> allowedTypes = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", 
            "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/zip",
            "video/mp4",
            "audio/mpeg"
        );

        boolean isAllowed = allowedTypes.contains(contentType);
        if (!isAllowed) {
            log.warn("File type not allowed: {} for file: {}", contentType, file.getOriginalFilename());
        }
        
        return isAllowed;
    }

    /**
     * ✅ NEW: Validate total file size for batch upload
     */
    private void validateTotalFileSize(List<MultipartFile> files) {
        long maxTotalSize = 50 * 1024 * 1024; // 50MB limit
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        
        if (totalSize > maxTotalSize) {
            throw new IllegalArgumentException(
                String.format("Total file size (%d MB) exceeds maximum allowed (%d MB)", 
                    totalSize / (1024 * 1024), maxTotalSize / (1024 * 1024))
            );
        }
    }

    /**
     * ✅ NEW: Get post type for directory organization
     */
    private String getPostType(Post post) {
        String className = post.getClass().getSimpleName().toLowerCase();
        
        // Map class names to directory names
        Map<String, String> typeMapping = Map.of(
            "topic", "topics",
            "reply", "replies",
            "question", "questions" // legacy support
        );
        
        return typeMapping.getOrDefault(className, "posts");
    }

    /**
     * ✅ OPTIMIZED: Batch delete files from storage
     */
    private void batchDeleteFiles(List<String> fileUrls) {
        if (fileUrls.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (String fileUrl : fileUrls) {
            try {
                fileStorageService.deleteFile(fileUrl);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to delete file from storage: {}", fileUrl, e);
                failureCount++;
            }
        }

        if (failureCount > 0) {
            log.warn("Failed to delete {} files from storage. {} files deleted successfully.", 
                     failureCount, successCount);
        } else {
            log.debug("Successfully deleted {} files from storage", successCount);
        }
    }

    /**
     * ✅ OPTIMIZED: Batch delete attachments from database
     */
    private void batchDeleteAttachments(List<Integer> attachmentIds) {
        try {
            attachmentRepository.deleteAllById(attachmentIds);
            log.debug("Successfully deleted {} attachment records from database", attachmentIds.size());
        } catch (Exception e) {
            log.error("Failed to delete attachment records from database", e);
            // Don't rethrow - we want to continue even if database cleanup fails for some
        }
    }

    /**
     * ✅ NEW: Convert file type statistics to readable format
     */
    private Map<String, Long> convertFileTypeStats(List<Object[]> fileTypeStats) {
        return fileTypeStats.stream()
                .collect(Collectors.toMap(
                    stat -> (String) stat[0],
                    stat -> (Long) stat[1]
                ));
    }

    /**
     * ✅ NEW: Find duplicate files by content hash (if implemented)
     */
    @Transactional(readOnly = true)
    public List<Attachment> findDuplicateFiles(String fileHash) {
        // This method requires adding a fileHash field to the Attachment entity
        // return attachmentRepository.findByFileHash(fileHash);
        return Collections.emptyList(); // Placeholder for future implementation
    }

    /**
     * ✅ NEW: Get attachments by post IDs (batch operation)
     */
    @Transactional(readOnly = true)
    public Map<Integer, List<Attachment>> getAttachmentsByPostIds(List<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Attachment> attachments = attachmentRepository.findByPostIdIn(postIds);
        
        return attachments.stream()
                .collect(Collectors.groupingBy(
                    attachment -> attachment.getPost().getId()
                ));
    }

    /**
     * ✅ NEW: Update attachment metadata
     */
    @Transactional
    public Attachment updateAttachmentMetadata(Integer attachmentId, String newFileName) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with ID: " + attachmentId));

        attachment.setFileName(newFileName);
        return attachmentRepository.save(attachment);
    }
}
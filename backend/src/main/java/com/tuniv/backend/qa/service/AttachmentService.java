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

    // --- FIX: Change the return type from void to List<Attachment> ---
    public List<Attachment> saveAttachments(List<MultipartFile> files, Integer postId, String postType) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Attachment> savedAttachments = new ArrayList<>();

        List<MultipartFile> validFiles = files.stream()
            .filter(Objects::nonNull)
            .filter(file -> !file.isEmpty())
            .collect(Collectors.toList());

        for (MultipartFile file : validFiles) {
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
                throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        return savedAttachments;
    }
}
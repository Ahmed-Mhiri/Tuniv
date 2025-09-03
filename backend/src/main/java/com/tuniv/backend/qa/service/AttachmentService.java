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
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.repository.AttachmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final FileStorage fileStorageService;
    private final AttachmentRepository attachmentRepository;

    // ✅ CHANGE: The method now accepts a Post object.
    public List<Attachment> saveAttachments(List<MultipartFile> files, Post post) {
        if (files == null || post == null || post.getId() == null) {
            return Collections.emptyList();
        }
        
        List<Attachment> savedAttachments = new ArrayList<>();

        List<MultipartFile> validFiles = files.stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getSize() > 0)
                .collect(Collectors.toList());

        for (MultipartFile file : validFiles) {
            try {
                // ✅ CHANGE: Determine subdirectory from the Post's class name.
                String postType = post.getClass().getSimpleName().toLowerCase();
                String subDirectory = postType + "s";
                String fileUrl = fileStorageService.storeFile(file, subDirectory);

                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setFileType(file.getContentType());
                attachment.setFileSize(file.getSize());
                
                // ✅ CHANGE: Set the entire Post object.
                attachment.setPost(post); 

                savedAttachments.add(attachmentRepository.save(attachment));
            } catch (Exception e) {
                throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        return savedAttachments;
    }
}
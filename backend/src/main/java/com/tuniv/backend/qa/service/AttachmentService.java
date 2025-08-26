package com.tuniv.backend.qa.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tuniv.backend.filestorage.service.FileStorageService;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.repository.AttachmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final FileStorageService fileStorageService;
    private final AttachmentRepository attachmentRepository;

    public void saveAttachments(List<MultipartFile> files, Integer postId, String postType) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                String fileUrl = fileStorageService.uploadFile(file.getBytes(), file.getOriginalFilename());

                Attachment attachment = new Attachment();
                attachment.setFileName(file.getOriginalFilename());
                attachment.setFileUrl(fileUrl);
                attachment.setFileType(file.getContentType());
                attachment.setFileSize(file.getSize());
                attachment.setPostId(postId);
                attachment.setPostType(postType);

                attachmentRepository.save(attachment);
            } catch (IOException e) {
                // In a real app, you might want a more specific exception
                throw new RuntimeException("Failed to upload file: " + e.getMessage());
            }
        }
    }
}
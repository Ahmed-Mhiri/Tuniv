package com.tuniv.backend.qa.dto;

public record AttachmentDto(
        Integer attachmentId,
        String fileName,
        String fileUrl,
        String fileType
) {}
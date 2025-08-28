package com.tuniv.backend.qa.dto;

public record AttachmentDto(
    String fileName,
    String fileUrl,
    String fileType
) {}
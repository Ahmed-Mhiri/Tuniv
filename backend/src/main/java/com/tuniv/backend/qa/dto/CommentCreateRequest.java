package com.tuniv.backend.qa.dto;

public record CommentCreateRequest(
    // No validation annotations here
    String body,
    
    Integer parentCommentId
) {}
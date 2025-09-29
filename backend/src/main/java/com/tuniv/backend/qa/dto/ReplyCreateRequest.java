package com.tuniv.backend.qa.dto;

public record ReplyCreateRequest(
    String body,
    Integer parentReplyId
) {}

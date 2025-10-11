package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

public record ReplyResponseDto(
    Integer id,
    String body,
    UserDto author,
    Instant createdAt,
    Instant editedAt, // ✅ ADDED
    int score,
    String currentUserVote,
    Integer topicId,
    Integer parentReplyId,
    boolean isSolution, // ✅ ADDED
    List<AttachmentDto> attachments,
    List<ReplyResponseDto> childReplies
) {}
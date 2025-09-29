package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

public record ReplyResponseDto(
    Integer id,
    String body,
    UserDto author,
    Instant createdAt,
    int score,
    String currentUserVote, // "UPVOTE", "DOWNVOTE", or null
    Integer topicId,
    Integer parentReplyId,
    List<AttachmentDto> attachments,
    List<ReplyResponseDto> childReplies
) {}

package com.tuniv.backend.qa.dto;

import java.time.Instant;
import java.util.List;

import com.tuniv.backend.qa.model.TopicType;

public record TopicResponseDto(
    Integer id,
    String title,
    String body,
    TopicType topicType,
    boolean isSolved,
    UserDto author,
    Instant createdAt,
    Instant editedAt, // ✅ ADDED: To show if/when a topic was edited
    int score,
    String currentUserVote, // "UPVOTE", "DOWNVOTE", or null
    int replyCount,
    int viewCount, // ✅ ADDED: To display engagement
    SolutionInfoDto acceptedSolution,
    ContainerInfoDto container,
    List<String> tags,
    List<AttachmentDto> attachments,
    List<ReplyResponseDto> replies
) {}
package com.tuniv.backend.qa.mapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.qa.dto.AttachmentDto;
import com.tuniv.backend.qa.dto.ContainerInfoDto;
import com.tuniv.backend.qa.dto.ReplyResponseDto;
import com.tuniv.backend.qa.dto.SolutionInfoDto;
import com.tuniv.backend.qa.dto.TopicResponseDto;
import com.tuniv.backend.qa.dto.TopicSummaryDto;
import com.tuniv.backend.qa.dto.UserDto;
import com.tuniv.backend.qa.model.Attachment;
import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.shared.model.ContainerType;
import com.tuniv.backend.user.model.User;

@Component
public class TopicMapper {

    public static TopicResponseDto buildTopicResponseDto(Topic topic, UserDetailsImpl currentUser, Map<Integer, Integer> currentUserVotes) {
        return new TopicResponseDto(
            topic.getId(),
            topic.getTitle(),
            topic.getBody(),
            topic.getTopicType(),
            topic.isSolved(),
            toUserDto(topic.getAuthor()),
            topic.getCreatedAt(),
            topic.getScore(),
            getVoteType(currentUserVotes.get(topic.getId())),
            topic.getReplies().size(),
            toSolutionInfoDto(topic.getAcceptedSolution()),
            toContainerInfoDto(topic),
            toTagNames(topic.getTags()),
            toAttachmentDtos(topic.getAttachments()),
            toReplyResponseDtos(topic.getReplies(), currentUser, currentUserVotes)
        );
    }

    public static TopicSummaryDto toTopicSummaryDto(Topic topic, String currentUserVote, Integer containerId, String containerName) {
        return new TopicSummaryDto(
            topic.getId(),
            topic.getTitle(),
            topic.getTopicType(),
            topic.getAuthor().getUserId(),
            topic.getAuthor().getUsername(),
            topic.getCreatedAt(),
            topic.getScore(),
            topic.getReplies().size(),
            currentUserVote,
            topic.isSolved(),
            containerId,
            containerName,
            toTagNames(topic.getTags())
        );
    }

    public static ReplyResponseDto toReplyResponseDto(Reply reply, UserDetailsImpl currentUser, Map<Integer, Integer> currentUserVotes) {
        return new ReplyResponseDto(
            reply.getId(),
            reply.getBody(),
            toUserDto(reply.getAuthor()),
            reply.getCreatedAt(),
            reply.getScore(),
            getVoteType(currentUserVotes.get(reply.getId())),
            reply.getTopic().getId(),
            reply.getParentReply() != null ? reply.getParentReply().getId() : null,
            toAttachmentDtos(reply.getAttachments()),
            toReplyResponseDtos(reply.getChildReplies(), currentUser, currentUserVotes)
        );
    }

    // Helper methods
    private static UserDto toUserDto(User user) {
    if (user == null) return null;
    return new UserDto(
        user.getUserId(),  // ✅ FIXED: Use getUserId() instead of getId()
        user.getUsername(),
        user.getReputationScore(),
        user.getProfilePhotoUrl() // ✅ FIXED: Use getProfilePhotoUrl() instead of getAvatarUrl()
    );
}

    private static SolutionInfoDto toSolutionInfoDto(Reply solution) {
        if (solution == null) return null;
        return new SolutionInfoDto(
            solution.getId(),
            solution.getBody(),
            toUserDto(solution.getAuthor()),
            solution.getCreatedAt()
        );
    }

    private static ContainerInfoDto toContainerInfoDto(Topic topic) {
    if (topic == null) return null;
    
    if (topic.getModule() != null) {
        return new ContainerInfoDto(
            topic.getModule().getModuleId(), // ✅ FIXED: Use getModuleId() instead of getId()
            topic.getModule().getName(),
            ContainerType.MODULE
        );
    } else if (topic.getCommunity() != null) {
        return new ContainerInfoDto(
            topic.getCommunity().getCommunityId(), // ✅ FIXED: Use getCommunityId() instead of getId()
            topic.getCommunity().getName(),
            ContainerType.COMMUNITY
        );
    }
    return null;
}

    private static List<String> toTagNames(Set<Tag> tags) {
        if (tags == null) return Collections.emptyList();
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    private static List<AttachmentDto> toAttachmentDtos(Set<Attachment> attachments) {
        if (attachments == null) return Collections.emptyList();
        return attachments.stream()
                .map(TopicMapper::toAttachmentDto)
                .collect(Collectors.toList());
    }

    public static AttachmentDto toAttachmentDto(Attachment attachment) {
        return new AttachmentDto(
            attachment.getAttachmentId(),
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getFileType(),
            attachment.getFileSize()
        );
    }

    private static List<ReplyResponseDto> toReplyResponseDtos(Set<Reply> replies, UserDetailsImpl currentUser, Map<Integer, Integer> currentUserVotes) {
        if (replies == null || replies.isEmpty()) return Collections.emptyList();
        
        // Filter top-level replies (no parent) and convert to DTOs
        return replies.stream()
                .filter(reply -> reply.getParentReply() == null)
                .sorted(Comparator.comparing(Reply::getCreatedAt))
                .map(reply -> toReplyResponseDto(reply, currentUser, currentUserVotes))
                .collect(Collectors.toList());
    }

    private static String getVoteType(Integer voteValue) {
        if (voteValue == null) return null;
        return voteValue == 1 ? "UPVOTE" : voteValue == -1 ? "DOWNVOTE" : null;
    }
}

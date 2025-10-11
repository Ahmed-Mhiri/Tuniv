package com.tuniv.backend.qa.mapper;

import com.tuniv.backend.qa.dto.*;
import com.tuniv.backend.qa.model.*;
import com.tuniv.backend.shared.model.ContainerType;
import com.tuniv.backend.user.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps Topic, Reply, and related entities to their corresponding DTOs.
 * Centralizes complex object graph assembly to keep services clean.
 * This component is designed to work with data fetched from repositories,
 * not from direct, lazy-loaded entity collections.
 */
@Component
public class TopicMapper {

    /**
     * The primary method for building the detailed Topic view.
     * It orchestrates the mapping of the topic and all its related, pre-fetched data.
     *
     * @param topic            The core Topic entity.
     * @param allReplies       A flat list of all replies belonging to the topic.
     * @param tags             A list of tags associated with the topic.
     * @param attachments      A list of attachments for the topic.
     * @param currentUserVotes A map of the current user's votes [postId -> voteType].
     * @return A fully assembled TopicResponseDto.
     */
    public TopicResponseDto toTopicResponseDto(
        Topic topic,
        List<Reply> allReplies,
        List<Tag> tags,
        List<Attachment> attachments,
        Map<Integer, String> currentUserVotes) {

        List<AttachmentDto> attachmentDtos = attachments.stream()
            .map(this::toAttachmentDto)
            .collect(Collectors.toList());

        List<String> tagNames = tags.stream()
            .map(Tag::getName)
            .collect(Collectors.toList());

        // ✅ Efficiently build the nested reply tree from the flat list
        List<ReplyResponseDto> nestedReplies = buildReplyTree(allReplies, currentUserVotes);

        return new TopicResponseDto(
            topic.getId(),
            topic.getTitle(),
            topic.getBody(),
            topic.getTopicType(),
            topic.isSolved(),
            toUserDto(topic.getAuthor()),
            topic.getCreatedAt(),
            topic.getEditedAt(),
            topic.getScore(),
            currentUserVotes.get(topic.getId()),
            topic.getReplyCount(), // ✅ Use denormalized count
            topic.getViewCount(),  // ✅ Use denormalized count
            toSolutionInfoDto(topic.getAcceptedSolution()),
            toContainerInfoDto(topic),
            tagNames,
            attachmentDtos,
            nestedReplies
        );
    }

    /**
     * Maps a Topic to its summary DTO, used in lists.
     *
     * @param topic           The Topic entity.
     * @param tags            The pre-fetched list of tags for this topic.
     * @param currentUserVote The current user's vote status for this topic.
     * @return A TopicSummaryDto.
     */
    public TopicSummaryDto toTopicSummaryDto(Topic topic, List<Tag> tags, String currentUserVote) {
        return new TopicSummaryDto(
            topic.getId(),
            topic.getTitle(),
            topic.getTopicType(),
            topic.getAuthor().getUserId(),
            topic.getAuthor().getUsername(),
            topic.getCreatedAt(),
            topic.getScore(),
            topic.getReplyCount(), // ✅ Use denormalized count
            currentUserVote,
            topic.isSolved(),
            getContainerId(topic),
            getContainerName(topic),
            tags.stream().map(Tag::getName).collect(Collectors.toList())
        );
    }

    //<editor-fold desc="Tree Building and Helper Methods">

    /**
     * Efficiently builds a nested reply tree from a flat list. This avoids N+1 query problems
     * and deep recursion issues that can occur with lazy-loaded collections.
     *
     * @param flatReplies      A list of ALL replies for a single topic.
     * @param currentUserVotes A map of the current user's votes.
     * @return A list of only the top-level replies, with their children nested correctly.
     */
    private List<ReplyResponseDto> buildReplyTree(List<Reply> flatReplies, Map<Integer, String> currentUserVotes) {
        if (flatReplies == null || flatReplies.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Create a map of all reply DTOs, keyed by their ID, for quick access.
        Map<Integer, ReplyResponseDto> replyDtoMap = flatReplies.stream()
            .collect(Collectors.toMap(Reply::getId, reply -> toReplyResponseDto(reply, currentUserVotes)));

        // 2. Group the DTOs by their parent's ID.
        Map<Integer, List<ReplyResponseDto>> childrenByParentId = flatReplies.stream()
            .filter(reply -> reply.getParentReply() != null)
            .collect(Collectors.groupingBy(
                reply -> reply.getParentReply().getId(),
                Collectors.mapping(reply -> replyDtoMap.get(reply.getId()), Collectors.toList())
            ));

        // 3. Attach the children lists to their respective parents.
        replyDtoMap.values().forEach(dto -> 
            dto.childReplies().addAll(childrenByParentId.getOrDefault(dto.id(), new ArrayList<>()))
        );

        // 4. Return only the top-level replies (those without a parent).
        return replyDtoMap.values().stream()
            .filter(dto -> dto.parentReplyId() == null)
            .collect(Collectors.toList());
    }

    /**
     * Helper to map a single Reply entity to its DTO.
     * Initializes `childReplies` as an empty list, which is populated by the `buildReplyTree` method.
     */
    private ReplyResponseDto toReplyResponseDto(Reply reply, Map<Integer, String> currentUserVotes) {
        // Here, you would fetch attachments for the reply if they were needed on a per-reply basis.
        // For simplicity, we assume attachments are primarily on the main topic.
        List<AttachmentDto> attachments = new ArrayList<>();

        return new ReplyResponseDto(
            reply.getId(),
            reply.getBody(),
            toUserDto(reply.getAuthor()),
            reply.getCreatedAt(),
            reply.getEditedAt(),
            reply.getScore(),
            currentUserVotes.get(reply.getId()),
            reply.getTopic().getId(),
            reply.getParentReply() != null ? reply.getParentReply().getId() : null,
            reply.isSolution(),
            attachments,
            new ArrayList<>() // Children are populated by the tree-building algorithm
        );
    }
    
    private UserDto toUserDto(User user) {
        if (user == null) return null;
        return new UserDto(
            user.getUserId(),
            user.getUsername(),
            user.getReputationScore(),
            user.getProfilePhotoUrl()
        );
    }

    private AttachmentDto toAttachmentDto(Attachment attachment) {
        if (attachment == null) return null;
        return new AttachmentDto(
            attachment.getAttachmentId(),
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getFileType(),
            attachment.getFileSize()
        );
    }

    private SolutionInfoDto toSolutionInfoDto(Reply solution) {
        if (solution == null) return null;
        return new SolutionInfoDto(
            solution.getId(),
            solution.getBody(),
            toUserDto(solution.getAuthor()),
            solution.getCreatedAt()
        );
    }

    private ContainerInfoDto toContainerInfoDto(Topic topic) {
        if (topic.getModule() != null) {
            return new ContainerInfoDto(topic.getModule().getModuleId(), topic.getModule().getName(), ContainerType.MODULE);
        }
        if (topic.getCommunity() != null) {
            return new ContainerInfoDto(topic.getCommunity().getCommunityId(), topic.getCommunity().getName(), ContainerType.COMMUNITY);
        }
        return null;
    }

    private Integer getContainerId(Topic topic) {
        if (topic.getModule() != null) return topic.getModule().getModuleId();
        if (topic.getCommunity() != null) return topic.getCommunity().getCommunityId();
        return null;
    }
    
    private String getContainerName(Topic topic) {
        if (topic.getModule() != null) return topic.getModule().getName();
        if (topic.getCommunity() != null) return topic.getCommunity().getName();
        return null;
    }

    //</editor-fold>
}
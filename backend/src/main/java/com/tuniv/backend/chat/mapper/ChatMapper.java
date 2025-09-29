package com.tuniv.backend.chat.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.qa.dto.AttachmentDto;
import com.tuniv.backend.qa.mapper.TopicMapper;
import com.tuniv.backend.qa.model.Attachment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    // No TopicMapper field needed here since its methods are static

    public ChatMessageDto toChatMessageDto(Message message, List<Reaction> reactions, String currentUsername) {
        if (message == null) {
            return null;
        }

        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(message.getId());
        dto.setDeleted(message.isDeleted());
        dto.setSenderUsername(message.getAuthor() != null ? message.getAuthor().getUsername() : "Unknown User");
        dto.setSentAt(message.getSentAt() != null ? message.getSentAt().toString() : message.getCreatedAt().toString());

        if (message.isDeleted()) {
            dto.setContent("This message was deleted.");
            dto.setAttachments(Collections.emptyList());
            dto.setReactions(Collections.emptyList());
        } else {
            dto.setContent(message.getBody());
            dto.setAttachments(mapAttachmentsToDto(message.getAttachments()));
            dto.setReactions(mapReactionsToDto(reactions, currentUsername));
        }
        return dto;
    }

    public ChatMessageDto toChatMessageDto(Message message, List<Reaction> reactions, String currentUsername, Long clientTempId) {
        ChatMessageDto dto = toChatMessageDto(message, reactions, currentUsername);
        if (dto != null) {
            dto.setClientTempId(clientTempId);
        }
        return dto;
    }

    /**
     * Helper method to map attachments.
     */
    private List<AttachmentDto> mapAttachmentsToDto(Set<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .map(TopicMapper::toAttachmentDto) // Correctly uses static method
                .collect(Collectors.toList());
    }

    /**
     * Helper method to group and map reactions to their DTO representation.
     */
    private List<ReactionDto> mapReactionsToDto(List<Reaction> reactions, String currentUsername) {
        if (reactions == null || reactions.isEmpty()) {
            return Collections.emptyList();
        }
        return reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji))
                .entrySet().stream()
                .map(entry -> {
                    String emoji = entry.getKey();
                    List<Reaction> reactionGroup = entry.getValue();
                    List<String> usernames = reactionGroup.stream()
                            .map(r -> r.getUser().getUsername())
                            .collect(Collectors.toList());
                    boolean currentUserReacted = currentUsername != null && usernames.contains(currentUsername);
                    return new ReactionDto(emoji, reactionGroup.size(), usernames, currentUserReacted);
                })
                .collect(Collectors.toList());
    }
}
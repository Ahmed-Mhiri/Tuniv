package com.tuniv.backend.chat.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.Reaction;
import com.tuniv.backend.qa.mapper.QAMapper;

public class ChatMapper {

    /**
     * ✅ UPDATED: The mapper now accepts a List<Reaction> as an argument,
     * since the Message entity no longer holds this collection.
     */
    public static ChatMessageDto toChatMessageDto(Message message, List<Reaction> reactions, String currentUsername) {
        if (message == null) {
            return null;
        }

        ChatMessageDto dto = new ChatMessageDto();
        dto.setMessageId(message.getId());
        dto.setDeleted(message.isDeleted());
        dto.setSenderUsername(message.getAuthor() != null ? message.getAuthor().getUsername() : "Unknown User");
        dto.setSentAt(message.getSentAt() != null ? message.getSentAt().toString() : message.getCreatedAt().toString());

        // Handle deleted content
        if (message.isDeleted()) {
            dto.setContent("This message was deleted.");
            dto.setAttachments(Collections.emptyList());
            dto.setReactions(Collections.emptyList()); // Also clear reactions
        } else {
            dto.setContent(message.getBody());
            dto.setAttachments(
                message.getAttachments() != null ?
                message.getAttachments().stream()
                       .map(QAMapper::toAttachmentDto)
                       .collect(Collectors.toList()) :
                Collections.emptyList()
            );

            // Handle reactions from the passed-in list
            if (reactions != null) {
                dto.setReactions(
                    reactions.stream()
                        .collect(Collectors.groupingBy(Reaction::getEmoji)) // Group by emoji from Reaction
                        .entrySet().stream()
                        .map(entry -> {
                            List<Reaction> reactionGroup = entry.getValue();
                            List<String> usernames = reactionGroup.stream()
                                    .map(r -> r.getUser().getUsername())
                                    .collect(Collectors.toList());
                            boolean currentUserReacted = currentUsername != null && usernames.contains(currentUsername);
                            return new ReactionDto(entry.getKey(), reactionGroup.size(), usernames, currentUserReacted);
                        })
                        .collect(Collectors.toList())
                );
            }
        }
        return dto;
    }

    // ✅ UPDATED: Helper overload to accept the reactions list
    public static ChatMessageDto toChatMessageDto(Message message, List<Reaction> reactions, String currentUsername, Long clientTempId) {
        ChatMessageDto dto = toChatMessageDto(message, reactions, currentUsername);
        if (dto != null) {
            dto.setClientTempId(clientTempId);
        }
        return dto;
    }
}
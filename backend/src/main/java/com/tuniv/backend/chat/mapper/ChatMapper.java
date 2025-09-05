package com.tuniv.backend.chat.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tuniv.backend.chat.dto.ChatMessageDto;
import com.tuniv.backend.chat.dto.ReactionDto;
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.model.MessageReaction;
import com.tuniv.backend.qa.mapper.QAMapper;

public class ChatMapper {

    // ✅ PRIMARY aapper: Handles everything except the clientTempId
    public static ChatMessageDto toChatMessageDto(Message message, String currentUsername) {
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
        } else {
            dto.setContent(message.getBody());
            dto.setAttachments(
                message.getAttachments() != null ?
                message.getAttachments().stream()
                       .map(QAMapper::toAttachmentDto) // Assuming QAMapper exists and is correct
                       .collect(Collectors.toList()) :
                Collections.emptyList()
            );
        }

        // Handle reactions
        if (message.getReactions() != null) {
            dto.setReactions(
                message.getReactions().stream()
                    .collect(Collectors.groupingBy(MessageReaction::getEmoji))
                    .entrySet().stream()
                    .map(entry -> {
                        List<MessageReaction> reactions = entry.getValue();
                        List<String> usernames = reactions.stream()
                                .map(r -> r.getUser().getUsername())
                                .collect(Collectors.toList());
                        boolean currentUserReacted = currentUsername != null && usernames.contains(currentUsername);
                        return new ReactionDto(entry.getKey(), reactions.size(), usernames, currentUserReacted);
                    })
                    .collect(Collectors.toList())
            );
        }

        return dto;
    }

    // ✅ HELPER overload: For convenience when clientTempId is also needed
    public static ChatMessageDto toChatMessageDto(Message message, String currentUsername, Long clientTempId) {
        ChatMessageDto dto = toChatMessageDto(message, currentUsername);
        if (dto != null) {
            dto.setClientTempId(clientTempId);
        }
        return dto;
    }
}
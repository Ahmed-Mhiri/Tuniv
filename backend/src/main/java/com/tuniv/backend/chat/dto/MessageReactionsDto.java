package com.tuniv.backend.chat.dto;

@Getter
@Setter
public class MessageReactionUpdateDto {
    private Integer messageId;
    private ReactionDto reaction;
    private ReactionAction action; // Changed from String to enum
}
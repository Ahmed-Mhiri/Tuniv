package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageReactionUpdateDto {
    private Integer messageId;
    private ReactionDto reaction;
    private ReactionAction action; // This now uses the DTO enum
}
package com.tuniv.backend.chat.dto;

import lombok.AllArgsConstructor; // ✅ ADDED
import lombok.Getter;
import lombok.NoArgsConstructor; // ✅ ADDED
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // ✅ ADDED
@AllArgsConstructor // ✅ ADDED: Creates a constructor with all fields
public class MessageReactionUpdateDto {
    private Integer messageId;
    private ReactionDto reaction;
    private ReactionAction action;
}
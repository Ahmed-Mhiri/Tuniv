package com.tuniv.backend.chat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReactionDto {
    private String emoji;
    private long count;
    private List<String> users; // List of usernames who used this emoji
    private boolean reactedByCurrentUser; // To help the frontend toggle state
}

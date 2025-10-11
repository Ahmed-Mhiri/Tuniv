package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGroupInfoRequest {
    private String title;
    private String description; // If you add this field to Conversation entity
    private String iconUrl;    // If you add this field to Conversation entity
}
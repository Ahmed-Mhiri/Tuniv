package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartConversationRequestDto {
    private Integer targetUserId;
}
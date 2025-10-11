package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequestDto {
    private String emoji;
    private Integer skinTone;
    private String customText;
}

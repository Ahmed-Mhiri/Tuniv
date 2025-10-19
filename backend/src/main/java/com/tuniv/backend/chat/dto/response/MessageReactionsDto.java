package com.tuniv.backend.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionsDto {
    private Integer messageId;
    private List<ReactionDto> reactions;
}

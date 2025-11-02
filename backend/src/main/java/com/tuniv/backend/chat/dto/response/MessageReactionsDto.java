package com.tuniv.backend.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import com.tuniv.backend.chat.dto.common.ReactionDto;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionsDto {
    private Integer messageId;
    private List<ReactionDto> reactions;
}

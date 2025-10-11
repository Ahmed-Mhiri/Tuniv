package com.tuniv.backend.chat.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class MessageThreadDto {
    private ChatMessageDto parentMessage;
    private List<ChatMessageDto> replies;
    private Integer replyCount;
    private boolean hasMoreReplies;
}
package com.tuniv.backend.chat.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tuniv.backend.chat.dto.response.ChatMessageDto;
import com.tuniv.backend.chat.dto.response.MessageThreadDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public interface MessageThreadService {

    /**
     * Gets a paginated list of replies to a specific parent message.
     */
    Page<ChatMessageDto> getMessageReplies(Integer parentMessageId, UserDetailsImpl currentUser, Pageable pageable);
    
    /**
     * Gets the full thread for a message (parent message + replies).
     */
    MessageThreadDto getMessageThread(Integer messageId, UserDetailsImpl currentUser);
}
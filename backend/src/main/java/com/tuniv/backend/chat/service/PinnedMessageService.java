package com.tuniv.backend.chat.service;

import java.util.List;

import com.tuniv.backend.chat.dto.PinnedMessageDto;
import com.tuniv.backend.config.security.services.UserDetailsImpl;

public interface PinnedMessageService {

    /**
     * Pins a message to the conversation.
     */
    PinnedMessageDto pinMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Unpins a message from the conversation.
     */
    void unpinMessage(Integer messageId, UserDetailsImpl currentUser);
    
    /**
     * Gets all pinned messages for a specific conversation.
     */
    List<PinnedMessageDto> getPinnedMessages(Integer conversationId, UserDetailsImpl currentUser);
    
    /**
     * Checks if a specific message is currently pinned.
     */
    boolean isMessagePinned(Integer messageId, UserDetailsImpl currentUser);
}
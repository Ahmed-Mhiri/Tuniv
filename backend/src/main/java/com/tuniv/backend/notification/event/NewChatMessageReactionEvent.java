package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent; // Adjust import as per your project

import com.tuniv.backend.chat.model.MessageReaction;

public class NewChatMessageReactionEvent extends ApplicationEvent {
    private final MessageReaction reaction;

    public NewChatMessageReactionEvent(Object source, MessageReaction reaction) {
        super(source);
        this.reaction = reaction;
    }

    public MessageReaction getReaction() {
        return reaction;
    }
}
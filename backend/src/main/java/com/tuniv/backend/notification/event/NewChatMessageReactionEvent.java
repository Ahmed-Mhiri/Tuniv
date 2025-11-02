package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent; // Adjust import as per your project

import com.tuniv.backend.chat.model.Reaction;

public class NewChatMessageReactionEvent extends ApplicationEvent {
    private final Reaction reaction;

    public NewChatMessageReactionEvent(Object source, Reaction reaction) {
        super(source);
        this.reaction = reaction;
    }

    public Reaction getReaction() {
        return reaction;
    }
}
package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.chat.model.Reaction;

public class NewReactionEvent extends ApplicationEvent {
    private final Reaction reaction;

    public NewReactionEvent(Object source, Reaction reaction) {
        super(source);
        this.reaction = reaction;
    }

    public Reaction getReaction() {
        return reaction;
    }
}
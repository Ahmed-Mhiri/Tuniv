package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.chat.model.Message;

import lombok.Getter;

@Getter
public class NewMessageEvent extends ApplicationEvent {
    private final Message message;

    public NewMessageEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }
}

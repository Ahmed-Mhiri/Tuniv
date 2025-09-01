package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Comment;

import lombok.Getter;

@Getter
public class NewCommentEvent extends ApplicationEvent {
    private final Comment comment;

    public NewCommentEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }
}

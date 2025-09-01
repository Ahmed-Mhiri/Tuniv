package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Answer;

import lombok.Getter;

@Getter
public class NewAnswerEvent extends ApplicationEvent {
    private final Answer answer;

    public NewAnswerEvent(Object source, Answer answer) {
        super(source);
        this.answer = answer;
    }
}
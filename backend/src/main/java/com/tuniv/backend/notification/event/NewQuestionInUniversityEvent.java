package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Question;

import lombok.Getter;

@Getter
public class NewQuestionInUniversityEvent extends ApplicationEvent {
    private final Question question;

    public NewQuestionInUniversityEvent(Object source, Question question) {
        super(source);
        this.question = question;
    }
}

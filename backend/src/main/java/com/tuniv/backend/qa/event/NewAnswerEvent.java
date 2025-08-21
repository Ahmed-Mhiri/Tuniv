package com.tuniv.backend.qa.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class NewAnswerEvent extends ApplicationEvent {
    
    private final String questionTitle;
    private final String questionAuthorEmail;
    private final String answerAuthorUsername;

    public NewAnswerEvent(Object source, String questionTitle, String questionAuthorEmail, String answerAuthorUsername) {
        super(source);
        this.questionTitle = questionTitle;
        this.questionAuthorEmail = questionAuthorEmail;
        this.answerAuthorUsername = answerAuthorUsername;
    }
}
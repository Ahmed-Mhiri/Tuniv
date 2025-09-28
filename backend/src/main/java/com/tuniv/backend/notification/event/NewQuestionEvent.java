package com.tuniv.backend.notification.event;

import com.tuniv.backend.qa.model.Question;

public class NewQuestionEvent {
    private final Question question;

    public NewQuestionEvent(Question question) {
        this.question = question;
    }

    public Question getQuestion() { return question; }
}

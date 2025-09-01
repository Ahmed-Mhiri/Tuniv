package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Answer;

import lombok.Getter;

@Getter
public class SolutionMarkedEvent extends ApplicationEvent {
    private final Answer answer;

    public SolutionMarkedEvent(Object source, Answer answer) {
        super(source);
        this.answer = answer;
    }
}

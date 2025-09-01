package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class UserJoinedUniversityEvent extends ApplicationEvent {
    private final User user;
    private final University university;

    public UserJoinedUniversityEvent(Object source, User user, University university) {
        super(source);
        this.user = user;
        this.university = university;
    }
}


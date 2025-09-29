package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class NewFollowerEvent extends ApplicationEvent {
    private final User follower;
    private final User followedUser;

    public NewFollowerEvent(Object source, User follower, User followedUser) {
        super(source);
        this.follower = follower;
        this.followedUser = followedUser;
    }
    // ✅ ADDED: Convenience constructor
    public NewFollowerEvent(User follower, User followedUser) {
        this(follower, follower, followedUser); // Using follower as source
    }
}
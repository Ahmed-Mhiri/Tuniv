package com.tuniv.backend.notification.event;

import com.tuniv.backend.user.model.User;

public class NewFollowerEvent {
    private final User follower;
    private final User followedUser;

    public NewFollowerEvent(User follower, User followedUser) {
        this.follower = follower;
        this.followedUser = followedUser;
    }

    public User getFollower() { return follower; }
    public User getFollowedUser() { return followedUser; }
}
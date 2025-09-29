package com.tuniv.backend.qa.model;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("TOPIC")
@NoArgsConstructor
public class TopicVote extends Vote {
    public TopicVote(User user, Post post, short value) {
        setUser(user);
        setPost(post);
        setValue(value);
    }
}

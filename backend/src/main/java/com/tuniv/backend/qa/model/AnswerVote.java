package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ANSWER") // Identifies this as an answer vote
@NoArgsConstructor
public class AnswerVote extends Vote {

    public AnswerVote(User user, Post post, short value) {
        this.setUser(user);
        this.setPost(post);
        this.setValue(value);
    }
}
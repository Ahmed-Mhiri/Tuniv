package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;


@Entity
@DiscriminatorValue("COMMENT") // Identifies this as a comment vote
@NoArgsConstructor
public class CommentVote extends Vote {

    public CommentVote(User user, Post post, short value) {
        this.setUser(user);
        this.setPost(post);
        this.setValue(value);
    }
}
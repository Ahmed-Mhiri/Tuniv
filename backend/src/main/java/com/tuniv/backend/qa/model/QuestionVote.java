package com.tuniv.backend.qa.model;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("QUESTION") // Identifies this as a question vote in the 'votes' table
@NoArgsConstructor
public class QuestionVote extends Vote {

    // The class is now empty because it inherits all fields and logic from Vote.
    // We only need a constructor for convenience.

    public QuestionVote(User user, Post post, short value) {
        this.setUser(user);
        this.setPost(post);
        this.setValue(value);
    }
}
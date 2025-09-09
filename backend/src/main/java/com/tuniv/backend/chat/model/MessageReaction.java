package com.tuniv.backend.chat.model;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("MESSAGE_REACTION")
@NoArgsConstructor
public class MessageReaction extends Reaction {

    // The class is now empty because it inherits all fields from Reaction.
    // We only need a constructor for convenience.
    
    public MessageReaction(User user, Post post, String emoji) {
        this.setUser(user);
        this.setPost(post);
        this.setEmoji(emoji);
    }
}
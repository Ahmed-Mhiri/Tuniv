package com.tuniv.backend.notification.event;
import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class NewCommentEvent extends ApplicationEvent {
    private final Reply comment;
    private final User author;

    public NewCommentEvent(Object source, Reply comment, User author) {
        super(source);
        this.comment = comment;
        this.author = author;
    }

    // ✅ ADDED: Convenience constructor
    public NewCommentEvent(Reply comment, User author) {
        this(comment, comment, author); // Using comment as source
    }
}

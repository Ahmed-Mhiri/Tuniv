package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class NewAnswerEvent extends ApplicationEvent {
    private final Reply answer;
    private final User author;

    public NewAnswerEvent(Object source, Reply answer, User author) {
        super(source);
        this.answer = answer;
        this.author = author;
    }

    // ✅ ADDED: Convenience constructor
    public NewAnswerEvent(Reply answer, User author) {
        this(answer, answer, author); // Using answer as source
    }
}

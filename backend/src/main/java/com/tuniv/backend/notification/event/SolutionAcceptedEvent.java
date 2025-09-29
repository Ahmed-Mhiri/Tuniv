package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class SolutionAcceptedEvent extends ApplicationEvent {
    private final Topic topic;
    private final Reply solution;
    private final User solutionAuthor;

    public SolutionAcceptedEvent(Object source, Topic topic, Reply solution, User solutionAuthor) {
        super(source);
        this.topic = topic;
        this.solution = solution;
        this.solutionAuthor = solutionAuthor;
    }
    // ✅ ADDED: Convenience constructor
    public SolutionAcceptedEvent(Topic topic, Reply solution, User solutionAuthor) {
        this(topic, topic, solution, solutionAuthor); // Using topic as source
    }
}

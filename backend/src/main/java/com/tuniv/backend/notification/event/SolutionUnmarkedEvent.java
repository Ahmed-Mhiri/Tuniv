package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Reply;
import com.tuniv.backend.qa.model.Topic;
import com.tuniv.backend.user.model.User;

import lombok.Getter;

@Getter
public class SolutionUnmarkedEvent extends ApplicationEvent {
    private final Topic topic;
    private final Reply previousSolution;
    private final User previousSolutionAuthor;

    public SolutionUnmarkedEvent(Object source, Topic topic, Reply previousSolution, User previousSolutionAuthor) {
        super(source);
        this.topic = topic;
        this.previousSolution = previousSolution;
        this.previousSolutionAuthor = previousSolutionAuthor;
    }

    // ✅ ADDED: Convenience constructor
    public SolutionUnmarkedEvent(Topic topic, Reply previousSolution, User previousSolutionAuthor) {
        this(topic, topic, previousSolution, previousSolutionAuthor); // Using topic as source
    }
}

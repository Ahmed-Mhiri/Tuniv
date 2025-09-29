package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.Topic;

import lombok.Getter;

@Getter
public class NewTopicEvent extends ApplicationEvent {
    private final Topic topic;

    public NewTopicEvent(Object source, Topic topic) {
        super(source);
        this.topic = topic;
    }

    // ✅ ADDED: Convenience constructor that uses the topic as source
    public NewTopicEvent(Topic topic) {
        this(topic, topic); // Using topic as both source and data
    }
}

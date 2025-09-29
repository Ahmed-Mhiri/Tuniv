package com.tuniv.backend.notification.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.qa.model.PostType;

import lombok.Getter;
@Getter
public class NewVoteEvent extends ApplicationEvent {
    private final Integer voterId;
    private final Integer authorId;
    private final PostType postType;
    private final Integer postId;
    private final String questionTitle;
    private final Integer questionId;

    public NewVoteEvent(Object source, Integer voterId, Integer authorId, PostType postType, 
                       Integer postId, String questionTitle, Integer questionId) {
        super(source);
        this.voterId = voterId;
        this.authorId = authorId;
        this.postType = postType;
        this.postId = postId;
        this.questionTitle = questionTitle;
        this.questionId = questionId;
    }

    // ✅ ADDED: Convenience constructor
    public NewVoteEvent(Integer voterId, Integer authorId, PostType postType, 
                       Integer postId, String questionTitle, Integer questionId) {
        this(new Object(), voterId, authorId, postType, postId, questionTitle, questionId);
    }
}

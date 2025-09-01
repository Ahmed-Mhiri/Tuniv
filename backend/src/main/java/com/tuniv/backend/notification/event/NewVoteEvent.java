package com.tuniv.backend.notification.event;


import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.notification.model.PostType;

import lombok.Getter;

@Getter
public class NewVoteEvent extends ApplicationEvent {
    private final Integer voterId;
    private final Integer authorId;
    private final PostType postType;
    private final Integer postId;
    private final String postTitleSnippet; // e.g., question title
    private final Integer questionId;      // To build the link

    public NewVoteEvent(Object source, Integer voterId, Integer authorId, PostType postType, Integer postId, String postTitleSnippet, Integer questionId) {
        super(source);
        this.voterId = voterId;
        this.authorId = authorId;
        this.postType = postType;
        this.postId = postId;
        this.postTitleSnippet = postTitleSnippet;
        this.questionId = questionId;
    }
}

package com.tuniv.backend.notification.model;

public enum NotificationType {
    // Answers & Comments
    NEW_ANSWER,
    ANSWER_MARKED_AS_SOLUTION,
    NEW_COMMENT_ON_ANSWER,
    NEW_REPLY_TO_COMMENT,

    // Votes
    NEW_VOTE_ON_QUESTION,
    NEW_VOTE_ON_ANSWER,
    NEW_VOTE_ON_COMMENT,

    // Social & Chat
    NEW_QUESTION_IN_UNI,
    NEW_CHAT_MESSAGE,
    
    // Welcome/System
    WELCOME_TO_UNIVERSITY
}

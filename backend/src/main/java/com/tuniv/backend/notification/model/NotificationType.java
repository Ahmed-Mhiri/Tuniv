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
    WELCOME_TO_UNIVERSITY,

    // ✅ NEW: Follow notifications
    NEW_FOLLOWER,
    
    // ✅ NEW: Content from followed entities
    NEW_QUESTION_FROM_FOLLOWED_USER,
    NEW_QUESTION_IN_FOLLOWED_COMMUNITY,
    NEW_QUESTION_IN_FOLLOWED_MODULE,
    NEW_QUESTION_WITH_FOLLOWED_TAG
}

package com.tuniv.backend.notification.model;

public enum NotificationType {
    // ✅ UPDATED: Topics & Replies
    NEW_ANSWER,
    ANSWER_MARKED_AS_SOLUTION,
    NEW_COMMENT_ON_ANSWER,
    NEW_REPLY_TO_COMMENT,
    
    // ✅ UPDATED: Votes
    NEW_VOTE_ON_QUESTION,
    NEW_VOTE_ON_ANSWER,
    NEW_VOTE_ON_COMMENT,

    // ✅ Social & Chat
    NEW_QUESTION_IN_UNI,
    NEW_CHAT_MESSAGE,
    NEW_REACTION_ON_CHAT_MESSAGE, 
    
    // ✅ Welcome/System
    WELCOME_TO_UNIVERSITY,

    // ✅ Follow notifications
    NEW_FOLLOWER,
    
    // ✅ Content from followed entities
    NEW_QUESTION_FROM_FOLLOWED_USER,
    NEW_QUESTION_IN_FOLLOWED_COMMUNITY,
    NEW_QUESTION_IN_FOLLOWED_MODULE,
    NEW_QUESTION_WITH_FOLLOWED_TAG,

    // ✅ NEW: Solution unmarked (optional)
    ANSWER_UNMARKED_AS_SOLUTION
}
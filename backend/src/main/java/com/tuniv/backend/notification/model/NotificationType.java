package com.tuniv.backend.notification.model;

// ========== ENUMS ==========
    public enum NotificationType {
        // Content Interactions
        NEW_ANSWER,
        ANSWER_MARKED_AS_SOLUTION,
        NEW_COMMENT_ON_ANSWER,
        NEW_REPLY_TO_COMMENT,
        NEW_VOTE_ON_QUESTION,
        NEW_VOTE_ON_ANSWER,
        NEW_VOTE_ON_COMMENT,

        // Social & Chat
        NEW_QUESTION_IN_UNI,
        NEW_CHAT_MESSAGE,
        NEW_REACTION_ON_CHAT_MESSAGE,
        NEW_FOLLOWER,

        // Content from followed entities
        NEW_QUESTION_FROM_FOLLOWED_USER,
        NEW_QUESTION_IN_FOLLOWED_COMMUNITY,
        NEW_QUESTION_IN_FOLLOWED_MODULE,
        NEW_QUESTION_WITH_FOLLOWED_TAG,

        // System & Welcome
        WELCOME_TO_UNIVERSITY,
        SYSTEM_ANNOUNCEMENT,
        VERIFICATION_COMPLETE,

        // Moderation
        CONTENT_REPORTED,
        CONTENT_APPROVED,
        CONTENT_REJECTED,

        // Solution updates
        ANSWER_UNMARKED_AS_SOLUTION
    }
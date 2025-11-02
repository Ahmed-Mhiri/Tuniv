package com.tuniv.backend.shared.model;

/**
 * Represents a container or type of content within the application.
 * Used for various purposes, including defining reportable content types
 * and topic container types.
 */
public enum ContainerType {
    // Original values (likely for topic containers)
    MODULE,
    COMMUNITY,

    // ✅ ADDED: Values for moderation reporting
    /**
     * A Topic, which is a type of Post.
     */
    TOPIC,

    /**
     * A Reply, which is a type of Post.
     */
    REPLY,

    /**
     * A User's profile.
     */
    USER_PROFILE,

    /**
     * A chat message.
     */
    MESSAGE
}
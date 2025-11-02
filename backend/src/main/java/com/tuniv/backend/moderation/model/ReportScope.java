package com.tuniv.backend.moderation.model;

/**
 * Defines the moderation queue or "scope" a report belongs to.
 * This is used for filtering reports for different moderator groups.
 */
public enum ReportScope {
    /**
     * Platform-wide reports (e.g., user profiles).
     * Handled by platform admins.
     */
    PLATFORM,

    /**
     * Reports related to university-specific content (e.g., Topics in a university context).
     * Handled by university moderators/admins.
     */
    UNIVERSITY,

    /**
     * Reports related to community-specific content (e.g., Topics in a community).
     * Handled by community moderators/admins.
     */
    COMMUNITY,

    /**
     * Reports related to chat messages or conversations.
     * Handled by chat moderators or context-aware admins (uni/community).
     */
    CHAT
}
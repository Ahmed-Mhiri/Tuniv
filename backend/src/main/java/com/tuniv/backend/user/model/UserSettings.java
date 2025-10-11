package com.tuniv.backend.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId // Uses the userId field as both the PK and FK
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========== NOTIFICATION PREFERENCES ==========
    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled", nullable = false)
    private boolean pushNotificationsEnabled = true;

    // Notification triggers
    @Column(name = "notify_on_new_message", nullable = false)
    private boolean notifyOnNewMessage = true;

    @Column(name = "notify_on_new_reply", nullable = false)
    private boolean notifyOnNewReply = true;

    @Column(name = "notify_on_new_follower", nullable = false)
    private boolean notifyOnNewFollower = true;

    @Column(name = "notify_on_mention", nullable = false)
    private boolean notifyOnMention = true;

    @Column(name = "notify_on_reaction", nullable = false)
    private boolean notifyOnReaction = true;

    @Column(name = "notify_on_followed_content", nullable = false)
    private boolean notifyOnFollowedContent = true;

    // ========== PRIVACY SETTINGS ==========
    @Column(name = "show_online_status", nullable = false)
    private boolean showOnlineStatus = true;

    @Column(name = "allow_search_engine_indexing", nullable = false)
    private boolean allowSearchEngineIndexing = true;

    @Column(name = "show_activity_status", nullable = false)
    private boolean showActivityStatus = true;

    @Column(name = "profile_visibility", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Column(name = "message_permissions", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessagePermissions messagePermissions = MessagePermissions.ANYONE;

    // ========== CONTENT & DISPLAY PREFERENCES ==========
    @Column(name = "content_language", nullable = false, length = 10)
    private String contentLanguage = "en";

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "theme", nullable = false, length = 20)
    private String theme = "light";

    @Column(name = "compact_mode_enabled", nullable = false)
    private boolean compactModeEnabled = false;

    // ========== EMAIL DIGEST PREFERENCES ==========
    @Column(name = "email_digest_frequency", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailDigestFrequency emailDigestFrequency = EmailDigestFrequency.WEEKLY;

    @Column(name = "receive_promotional_emails", nullable = false)
    private boolean receivePromotionalEmails = false;

    @Column(name = "receive_system_announcements", nullable = false)
    private boolean receiveSystemAnnouncements = true;

    // ========== CONTENT FILTERING ==========
    @Column(name = "safe_content_mode", nullable = false)
    private boolean safeContentMode = true;

    @Column(name = "hide_explicit_content", nullable = false)
    private boolean hideExplicitContent = true;

    @Column(name = "collapse_long_posts", nullable = false)
    private boolean collapseLongPosts = false;

    // Custom constructor
    public UserSettings(User user) {
        this.user = user;
        this.userId = user.getUserId();
    }
    


}
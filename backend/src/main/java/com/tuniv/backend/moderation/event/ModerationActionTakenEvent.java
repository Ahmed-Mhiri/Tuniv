package com.tuniv.backend.moderation.event;

import org.springframework.context.ApplicationEvent;

import com.tuniv.backend.moderation.dto.ModerationActionDto;
import com.tuniv.backend.user.model.User;

/**
 * Event published when a moderator executes an action (e.g., ban, delete).
 * This is used to decouple the moderation service from other domains.
 */
public class ModerationActionTakenEvent extends ApplicationEvent {

    private final ModerationActionDto action;
    private final User moderator;
    private final User targetUser;
    private final Object targetContent;

    public ModerationActionTakenEvent(
            Object source,
            ModerationActionDto action,
            User moderator,
            User targetUser,
            Object targetContent) {
        super(source);
        this.action = action;
        this.moderator = moderator;
        this.targetUser = targetUser;
        this.targetContent = targetContent;
    }

    // Add getters for all fields
    public ModerationActionDto getAction() { return action; }
    public User getModerator() { return moderator; }
    public User getTargetUser() { return targetUser; }
    public Object getTargetContent() { return targetContent; }
}
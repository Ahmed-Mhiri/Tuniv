package com.tuniv.backend.chat.model;

public enum DefaultConversationRoles {
    CONVERSATION_ADMIN("conversation_admin", "Conversation Admin", true),
    CONVERSATION_MODERATOR("conversation_moderator", "Conversation Moderator", true),
    CONVERSATION_MEMBER("conversation_member", "Conversation Member", true);

    private final String name;
    private final String displayName;
    private final boolean isSystemRole;

    DefaultConversationRoles(String name, String displayName, boolean isSystemRole) {
        this.name = name;
        this.displayName = displayName;
        this.isSystemRole = isSystemRole;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSystemRole() {
        return isSystemRole;
    }
}
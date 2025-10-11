package com.tuniv.backend.community.model;

public enum DefaultCommunityRoles {
    COMMUNITY_OWNER("community_owner", "Community Owner", true),
    COMMUNITY_ADMIN("community_admin", "Community Admin", true),
    COMMUNITY_MODERATOR("community_moderator", "Community Moderator", true),
    COMMUNITY_MEMBER("community_member", "Community Member", true);

    private final String name;
    private final String displayName;
    private final boolean isSystemRole;

    DefaultCommunityRoles(String name, String displayName, boolean isSystemRole) {
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
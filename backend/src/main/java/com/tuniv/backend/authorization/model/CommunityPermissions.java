package com.tuniv.backend.authorization.model;

public enum CommunityPermissions {
    COMMUNITY_SETTINGS_EDIT("community.settings.edit", "Edit community settings"),
    COMMUNITY_ROLE_MANAGE("community.role.manage", "Manage community roles"),
    COMMUNITY_MEMBER_INVITE("community.member.invite", "Invite members"),
    COMMUNITY_MEMBER_KICK("community.member.kick", "Remove members"),
    COMMUNITY_MEMBER_BAN("community.member.ban", "Ban members"),
    COMMUNITY_TOPIC_PIN("community.topic.pin", "Pin topics"),
    COMMUNITY_TOPIC_LOCK("community.topic.lock", "Lock topics"),
    COMMUNITY_REPORT_MANAGE("community.report.manage", "Manage community reports");

    private final String name;
    private final String description;

    CommunityPermissions(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

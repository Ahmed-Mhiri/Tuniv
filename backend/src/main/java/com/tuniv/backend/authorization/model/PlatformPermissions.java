package com.tuniv.backend.authorization.model;

public enum PlatformPermissions {
    PLATFORM_UNIVERSITY_CREATE("platform.university.create", "Create new universities"),
    PLATFORM_UNIVERSITY_MANAGE("platform.university.manage", "Manage universities"),
    PLATFORM_USER_IMPERSONATE("platform.user.impersonate", "Impersonate users"),
    PLATFORM_SETTINGS_MANAGE("platform.settings.manage", "Manage platform settings"),
    PLATFORM_ANALYTICS_VIEW("platform.analytics.view", "View platform analytics");

    private final String name;
    private final String description;

    PlatformPermissions(String name, String description) {
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
package com.tuniv.backend.authorization.model;

public enum UniversityPermissions {
    UNIVERSITY_SETTINGS_EDIT("university.settings.edit", "Edit university settings"),
    UNIVERSITY_ROLE_MANAGE("university.role.manage", "Manage university roles"),
    UNIVERSITY_USER_MANAGE("university.user.manage", "Manage university users"),
    UNIVERSITY_USER_SUSPEND("university.user.suspend", "Suspend university users"),
    UNIVERSITY_USER_VERIFY("university.user.verify", "Verify university users"),
    UNIVERSITY_COMMUNITY_MANAGE("university.community.manage", "Manage communities"),
    UNIVERSITY_MODULE_MANAGE("university.module.manage", "Manage modules"),
    UNIVERSITY_ANNOUNCEMENT_CREATE("university.announcement.create", "Create announcements"),
    UNIVERSITY_REPORT_MANAGE("university.report.manage", "Manage university reports");

    private final String name;
    private final String description;

    UniversityPermissions(String name, String description) {
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
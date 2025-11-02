package com.tuniv.backend.authorization.model;

public enum ChatPermissions {
    // Message permissions
    SEND_MESSAGES("send_messages", "Send messages in conversation"),
    DELETE_OWN_MESSAGES("delete_own_messages", "Delete own messages"),
    DELETE_ANY_MESSAGE("delete_any_message", "Delete any message"),
    EDIT_OWN_MESSAGES("edit_own_messages", "Edit own messages"),
    EDIT_ANY_MESSAGE("edit_any_message", "Edit any message"),
    PIN_MESSAGES("pin_messages", "Pin messages"),

    // Participant management
    ADD_PARTICIPANTS("add_participants", "Add new participants"),
    REMOVE_PARTICIPANTS("remove_participants", "Remove participants"),
    MANAGE_ROLES("manage_roles", "Manage participant roles"),

    // Conversation management
    EDIT_CONVERSATION_INFO("edit_conversation_info", "Edit conversation title and settings"),
    ARCHIVE_CONVERSATION("archive_conversation", "Archive conversation"),
    DELETE_CONVERSATION("delete_conversation", "Delete conversation"),

    // Moderation
    MUTE_PARTICIPANTS("mute_participants", "Mute participants temporarily"),
    BAN_PARTICIPANTS("ban_participants", "Ban participants from conversation"),
    
    // ✅ ADDED: Report management
    MANAGE_REPORTS("manage_reports", "View and manage chat reports");

    private final String name;
    private final String description;

    ChatPermissions(String name, String description) {
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
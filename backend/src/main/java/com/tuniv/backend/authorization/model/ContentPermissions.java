package com.tuniv.backend.authorization.model;

public enum ContentPermissions {
    POST_TOPIC_CREATE("post.topic.create", "Create topics"),
    POST_REPLY_CREATE("post.reply.create", "Create replies"),
    POST_EDIT_OWN("post.edit.own", "Edit own posts"),
    POST_EDIT_ANY("post.edit.any", "Edit any posts"),
    POST_DELETE_OWN("post.delete.own", "Delete own posts"),
    POST_DELETE_ANY("post.delete.any", "Delete any posts"),
    POST_VOTE_CAST("post.vote.cast", "Cast votes"),
    MESSAGE_SEND("message.send", "Send messages"),
    MESSAGE_DELETE_OWN("message.delete.own", "Delete own messages"),
    MESSAGE_DELETE_ANY("message.delete.any", "Delete any messages"),
    MESSAGE_REACT("message.react", "React to messages");

    private final String name;
    private final String description;

    ContentPermissions(String name, String description) {
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

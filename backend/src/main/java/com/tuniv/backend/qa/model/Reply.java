package com.tuniv.backend.qa.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("REPLY")
@Getter
@Setter
public class Reply extends VotablePost {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    @JsonBackReference("reply-children")
    private Reply parentReply;

    @OneToMany(mappedBy = "parentReply", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("reply-children")
    private Set<Reply> childReplies = new HashSet<>();

    // ✅ NEW: Set topic with automatic count update
    public void setTopic(Topic topic) {
        // Remove from old topic if exists and this is a top-level reply
        if (this.topic != null && this.parentReply == null) {
            this.topic.decrementReplyCount();
        }
        
        this.topic = topic;
        
        // Add to new topic if this is a top-level reply
        if (topic != null && this.parentReply == null) {
            topic.incrementReplyCount();
        }
    }

    // ✅ NEW: Set parent reply with topic count management
    public void setParentReply(Reply parentReply) {
        boolean wasTopLevel = this.parentReply == null;
        boolean willBeTopLevel = parentReply == null;
        
        // Update topic count if top-level status changes
        if (this.topic != null && wasTopLevel != willBeTopLevel) {
            if (wasTopLevel) {
                this.topic.decrementReplyCount(); // Becoming nested reply
            } else {
                this.topic.incrementReplyCount(); // Becoming top-level reply
            }
        }
        
        this.parentReply = parentReply;
        
        // Update child relationship if needed
        if (parentReply != null && !parentReply.getChildReplies().contains(this)) {
            parentReply.getChildReplies().add(this);
        }
    }

    // ✅ NEW: Convenience method to add child reply
    public void addChildReply(Reply childReply) {
        this.childReplies.add(childReply);
        childReply.setParentReply(this);
    }

    // ✅ NEW: Convenience method to remove child reply
    public void removeChildReply(Reply childReply) {
        this.childReplies.remove(childReply);
        childReply.setParentReply(null);
    }

    // ✅ NEW: Check if this reply is a top-level reply
    public boolean isTopLevel() {
        return this.parentReply == null;
    }
}
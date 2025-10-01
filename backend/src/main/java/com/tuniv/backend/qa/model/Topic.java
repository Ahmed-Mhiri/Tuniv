package com.tuniv.backend.qa.model;
import java.util.HashSet;
import java.util.Set;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.university.model.Module;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("TOPIC")
@Getter
@Setter
public class Topic extends VotablePost {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false)
    private TopicType topicType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_solution_id")
    private Reply acceptedSolution;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved = false;

    // ✅ NEW: Denormalized reply count for performance - eliminates N+1 queries
    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reply> replies = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // ✅ NEW: Helper methods for reply count management
    public void incrementReplyCount() {
        this.replyCount++;
    }

    public void decrementReplyCount() {
        this.replyCount = Math.max(0, this.replyCount - 1);
    }

    // ✅ NEW: Add reply with count update
    public void addReply(Reply reply) {
        this.replies.add(reply);
        reply.setTopic(this);
        if (reply.isTopLevel()) { // Only count top-level replies
            incrementReplyCount();
        }
    }

    // ✅ NEW: Remove reply with count update
    public void removeReply(Reply reply) {
        this.replies.remove(reply);
        reply.setTopic(null);
        if (reply.isTopLevel()) {
            decrementReplyCount();
        }
    }

    // ✅ NEW: Set accepted solution with validation
    public void setAcceptedSolution(Reply acceptedSolution) {
        this.acceptedSolution = acceptedSolution;
        if (acceptedSolution != null) {
            this.isSolved = true;
            // Ensure the accepted solution belongs to this topic
            if (!acceptedSolution.getTopic().equals(this)) {
                throw new IllegalArgumentException("Accepted solution must belong to this topic");
            }
        } else {
            this.isSolved = false;
        }
    }

    // ✅ NEW: Add tag with convenience method
    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getTopics().add(this);
    }

    // ✅ NEW: Remove tag with convenience method
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getTopics().remove(this);
    }

    // ✅ NEW: Check if topic has a specific tag
    public boolean hasTag(Tag tag) {
        return this.tags.contains(tag);
    }

    // ✅ NEW: Get approximate word count for body text
    public int getWordCount() {
        if (this.getBody() == null || this.getBody().trim().isEmpty()) {
            return 0;
        }
        return this.getBody().trim().split("\\s+").length;
    }
}
package com.tuniv.backend.qa.model;

import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "replies", indexes = {
    @Index(name = "idx_reply_topic", columnList = "topic_id, created_at"),
    @Index(name = "idx_reply_author", columnList = "user_id, created_at"),
    @Index(name = "idx_reply_parent", columnList = "parent_reply_id, created_at"),
    @Index(name = "idx_reply_solution", columnList = "is_solution, created_at DESC"),
    @Index(name = "idx_reply_depth", columnList = "topic_id, depth, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Reply extends VotablePost {

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== REPLY-SPECIFIC FIELDS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private Reply parentReply;

    // ❌ REMOVED: Dangerous childReplies collection
    // @OneToMany(mappedBy = "parentReply", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    // private Set<Reply> childReplies = new HashSet<>();

    @Column(name = "is_solution", nullable = false)
    private boolean isSolution = false;

    @Column(name = "depth", nullable = false)
    private Integer depth = 0;

    // ========== CONSTRUCTORS ==========
    public Reply(String body, User author, Topic topic, University universityContext) {
        this.setBody(body);
        this.setAuthor(author);
        this.topic = topic;
        this.depth = 0;

        if (universityContext != null) {
            this.setUniversityContext(universityContext);
        }
    }

    @Override
    public String getPostType() {
        return "REPLY";
    }
}
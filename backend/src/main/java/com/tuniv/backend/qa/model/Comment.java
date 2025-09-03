package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue; // <-- IMPORT ADDED
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // --- FIX: Add explicit column mapping to the primary key ---
    @Column(name = "comment_id")
    private Integer commentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp // <-- USE THIS annotation for auto-creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // <-- CHANGE type to Instant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    @JsonBackReference("answer-comments")
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-comments")
    private User author;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("comment-votes")
    private Set<CommentVote> votes = new HashSet<>();

    // --- FIX: Add self-referencing relationship for nesting ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @JsonBackReference("comment-children")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("comment-children")
    private Set<Comment> children = new HashSet<>();

    // --- FIX: Add relationship to the central attachments table ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", referencedColumnName = "comment_id", insertable = false, updatable = false)
    @Where(clause = "post_type = 'COMMENT'")
    private Set<Attachment> attachments = new HashSet<>();
    
}
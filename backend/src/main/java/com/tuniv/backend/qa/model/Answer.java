package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonBackReference; // <-- IMPORT
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType; // <-- IMPORT ADDED
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "answers")
@Getter
@Setter
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // --- FIX: Add explicit column mapping to the primary key ---
    @Column(name = "answer_id")
    private Integer answerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_solution", nullable = false)
    private Boolean isSolution = false;

    @CreationTimestamp // <-- USE THIS annotation for auto-creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // <-- CHANGE type to Instant
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonBackReference("question-answers")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-answers")
    private User author;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("answer-comments")
    private Set<Comment> comments = new HashSet<>();

    // --- FIX: Add relationship to votes ---
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("answer-votes")
    private Set<AnswerVote> votes = new HashSet<>();

    // --- FIX: Add relationship to the central attachments table ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", referencedColumnName = "answer_id", insertable = false, updatable = false)
    @Where(clause = "post_type = 'ANSWER'")
    private Set<Attachment> attachments = new HashSet<>();

    

    @UpdateTimestamp // <-- USE THIS annotation for auto-update timestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // <-- CHANGE type to Instant


}
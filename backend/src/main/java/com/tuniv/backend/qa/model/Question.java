package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "questions")
@Getter
@Setter
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // --- FIX: Add explicit column mapping to the primary key ---
    @Column(name = "question_id")
    private Integer questionId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp // <-- USE THIS annotation for auto-creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // <-- CHANGE type to Instant

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-questions")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonBackReference("module-questions")
    private Module module;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("question-answers")
    private Set<Answer> answers = new HashSet<>(); // <-- Initialize the Set

    // --- FIX: Add relationship to votes ---
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("question-votes")
    private Set<QuestionVote> votes = new HashSet<>();

    // --- FIX: Add relationship to the central attachments table ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "post_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    @Where(clause = "post_type = 'QUESTION'")
    private Set<Attachment> attachments = new HashSet<>();

}
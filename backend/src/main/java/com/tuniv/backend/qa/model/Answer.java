package com.tuniv.backend.qa.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType; // <-- IMPORT
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany; // <-- IMPORT ADDED
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("ANSWER") // ✅ This value is stored in the 'post_type' column
@Getter
@Setter
public class Answer extends Post { // ✅ EXTENDS Post

    // ❌ ID, author, createdAt, and attachments are REMOVED as they are now in Post.


    @Column(name = "is_solution", nullable = false)
    private Boolean isSolution = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonBackReference("question-answers")
    private Question question;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("answer-comments")
    private Set<Comment> comments = new HashSet<>();


    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
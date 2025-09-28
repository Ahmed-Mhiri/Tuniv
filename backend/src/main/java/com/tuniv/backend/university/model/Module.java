package com.tuniv.backend.university.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.qa.model.Question;

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
@Table(name = "modules")
@Getter
@Setter
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer moduleId;

    @Column(nullable = false)
    private String name;

    // ✅ NEW: Denormalized question count for performance
    @Column(name = "question_count", nullable = false)
    private int questionCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    @JsonBackReference("university-modules")
    private University university;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("module-questions")
    private Set<Question> questions;

    // ✅ Helper method to increment question count
    public void incrementQuestionCount() {
        this.questionCount++;
        // Also update parent university count
        if (this.university != null) {
            this.university.incrementQuestionCount();
        }
    }

    // ✅ Helper method to decrement question count
    public void decrementQuestionCount() {
        this.questionCount = Math.max(0, this.questionCount - 1);
        // Also update parent university count
        if (this.university != null) {
            this.university.decrementQuestionCount();
        }
    }
}
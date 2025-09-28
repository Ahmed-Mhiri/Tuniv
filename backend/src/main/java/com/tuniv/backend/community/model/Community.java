package com.tuniv.backend.community.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.university.model.University;
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
@Table(name = "communities")
@Getter
@Setter
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer communityId;

    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Denormalized count for performance
    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    // Denormalized question count for performance
    @Column(name = "question_count", nullable = false)
    private int questionCount = 0;

    @OneToMany(mappedBy = "community", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CommunityMembership> members = new HashSet<>();

    // ✅ Helper method to increment question count
    public void incrementQuestionCount() {
        this.questionCount++;
    }

    // ✅ Helper method to decrement question count
    public void decrementQuestionCount() {
        this.questionCount = Math.max(0, this.questionCount - 1);
    }

    // ✅ NEW: Helper method to increment member count
    public void incrementMemberCount() {
        this.memberCount++;
    }

    // ✅ NEW: Helper method to decrement member count
    public void decrementMemberCount() {
        this.memberCount = Math.max(0, this.memberCount - 1);
    }
}
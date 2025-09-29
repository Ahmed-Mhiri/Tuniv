package com.tuniv.backend.university.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "universities")
@Getter
@Setter
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer universityId;

    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(name = "email_domain", nullable = false, unique = true)
    private String emailDomain;

    // ✅ UPDATED: Renamed from question_count to topic_count
    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    // ✅ NEW: Denormalized member count for performance (optional but consistent)
    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @OneToMany(mappedBy = "university", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("university-modules")
    private Set<Module> modules;

    @OneToMany(mappedBy = "university", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("university-memberships")
    private Set<UniversityMembership> memberships;

    public void incrementTopicCount() {
        this.topicCount++;
    }

    public void decrementTopicCount() {
        this.topicCount = Math.max(0, this.topicCount - 1);
    }

    // ✅ Helper method to increment member count
    public void incrementMemberCount() {
        this.memberCount++;
    }

    // ✅ Helper method to decrement member count
    public void decrementMemberCount() {
        this.memberCount = Math.max(0, this.memberCount - 1);
    }
}
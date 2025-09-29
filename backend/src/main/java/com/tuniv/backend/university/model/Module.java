package com.tuniv.backend.university.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuniv.backend.qa.model.Topic;

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

    // ✅ MODIFIED: Renamed from question_count to topic_count
    @Column(name = "topic_count", nullable = false)
    private int topicCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    @JsonBackReference("university-modules")
    private University university;

    // ✅ MODIFIED: Relationship now points to Topic
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Topic> topics = new HashSet<>();

    // ✅ MODIFIED: Helper methods updated for topicCount
    public void incrementTopicCount() {
        this.topicCount++;
    }

    public void decrementTopicCount() {
        this.topicCount = Math.max(0, this.topicCount - 1);
    }
}
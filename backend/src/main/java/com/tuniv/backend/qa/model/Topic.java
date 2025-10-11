package com.tuniv.backend.qa.model;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "topics", indexes = {
    @Index(name = "idx_topic_title", columnList = "title"),
    @Index(name = "idx_topic_community", columnList = "community_id, created_at DESC"),
    @Index(name = "idx_topic_module", columnList = "module_id, created_at DESC"),
    @Index(name = "idx_topic_author", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_topic_last_activity", columnList = "last_activity_at DESC"),
    @Index(name = "idx_topic_pinned", columnList = "is_pinned, last_activity_at DESC"),
    @Index(name = "idx_topic_solved", columnList = "is_solved, created_at DESC"),
    @Index(name = "idx_topic_score", columnList = "score DESC, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class Topic extends VotablePost {

    // ========== OPTIMISTIC LOCKING ==========
    @Version
    private Long version;

    // ========== TOPIC-SPECIFIC FIELDS ==========
    @NotBlank(message = "Topic title cannot be empty")
    @Size(max = 255, message = "Topic title cannot exceed 255 characters")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "topic_type", nullable = false)
    private TopicType topicType;

    // ========== SOLUTION TRACKING ==========
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_solution_id")
    private Reply acceptedSolution;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved = false;

    @Column(name = "solution_awarded_at")
    private Instant solutionAwardedAt;

    // ========== MODERATION FIELDS ==========
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;

    @Column(name = "pinned_until")
    private Instant pinnedUntil;

    @Column(name = "locked_reason")
    private String lockedReason;

    // ========== ENGAGEMENT STATISTICS ==========
    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "share_count", nullable = false)
    private int shareCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount = 0;

    @Column(name = "participating_universities_count", nullable = false)
    private int participatingUniversitiesCount = 1;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    // ========== RELATIONSHIPS ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    // ❌ REMOVED: Collections - use repository queries instead
    // - Set<Reply> replies
    // - Set<Tag> tags (replaced by TopicTag entity)

    @ElementCollection
    @CollectionTable(
        name = "topic_participating_universities",
        joinColumns = @JoinColumn(name = "topic_id")
    )
    @Column(name = "university_id")
    private Set<Integer> participatingUniversityIds = new HashSet<>();

    // ========== CONSTRUCTORS ==========
    
    /**
     * Clean constructor without reaching through objects
     * All dependencies must be explicitly provided
     */
    public Topic(String title, String body, User author, TopicType topicType, University universityContext) {
        this.setBody(body);
        this.setAuthor(author);
        this.title = title;
        this.topicType = topicType;
        this.lastActivityAt = Instant.now();
        
        // Set university context explicitly (no more reaching through author)
        if (universityContext != null) {
            this.setUniversityContext(universityContext);
            this.participatingUniversityIds.add(universityContext.getUniversityId());
        }
    }

    /**
     * Constructor for community topics
     */
    public Topic(String title, String body, User author, TopicType topicType, 
                 University universityContext, Community community) {
        this(title, body, author, topicType, universityContext);
        this.community = community;
    }

    /**
     * Constructor for module topics  
     */
    public Topic(String title, String body, User author, TopicType topicType,
                 University universityContext, Module module) {
        this(title, body, author, topicType, universityContext);
        this.module = module;
    }

    @Override
    public String getPostType() {
        return "TOPIC";
    }
}
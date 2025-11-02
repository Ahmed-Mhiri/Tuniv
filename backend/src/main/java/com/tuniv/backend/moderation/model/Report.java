package com.tuniv.backend.moderation.model;

import java.time.Instant;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;

import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import static jakarta.persistence.DiscriminatorType.STRING;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_target", columnList = "target_type, target_id"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_created", columnList = "created_at DESC"),
        // ✅ ADDED: Indexes for filtering moderation queues
        @Index(name = "idx_report_scope_status", columnList = "scope, status"),
        @Index(name = "idx_report_university_status", columnList = "university_id, scope, status"),
        @Index(name = "idx_report_community_status", columnList = "community_id, scope, status"),
        @Index(name = "idx_report_conversation_status", columnList = "conversation_id, scope, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporter;

    // ========== POLYMORPHIC TARGET (Correct Hibernate 6 Syntax) ==========

    /**
     * This field stores the discriminator string (e.g., "POST", "USER").
     * The @AnyDiscriminator annotation defines the column type.
     * The @AnyDiscriminatorValue annotations map a string to an entity class.
     */
    @AnyDiscriminator(STRING)
    @AnyDiscriminatorValue(discriminator = "POST", entity = Post.class)
    @AnyDiscriminatorValue(discriminator = "USER", entity = User.class)
    @AnyDiscriminatorValue(discriminator = "COMMUNITY", entity = Community.class)
    @AnyDiscriminatorValue(discriminator = "MESSAGE", entity = Message.class)
    @Column(name = "target_type", length = 50, nullable = false)
    private String targetType;

    /**
     * This field holds the actual entity (e.g., a Post object) and is
     * linked via the foreign key in the @JoinColumn.
     */
    @Any
    @JoinColumn(name = "target_id", nullable = false)
    private Object target;

    @NotBlank
    @Size(max = 100)
    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Size(max = 1000)
    @Column(name = "details", length = 1000)
    private String details;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    // ✅ ADDED: Scope for categorizing the report
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private ReportScope scope;

    // ✅ ADDED: Denormalized IDs for filtering (NO JOINS)
    @Column(name = "university_id")
    private Integer universityId;

    @Column(name = "community_id")
    private Integer communityId;

    @Column(name = "conversation_id")
    private Integer conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

}
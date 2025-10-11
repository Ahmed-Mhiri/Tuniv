package com.tuniv.backend.university.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "university_memberships", indexes = {
    @Index(name = "idx_membership_user", columnList = "user_id"),
    @Index(name = "idx_membership_university", columnList = "university_id"),
    @Index(name = "idx_membership_status", columnList = "status"),
    @Index(name = "idx_membership_primary", columnList = "is_primary"),
    @Index(name = "idx_membership_role", columnList = "role_id"),
    @Index(name = "idx_membership_verified", columnList = "status, is_primary DESC"),
    @Index(name = "idx_membership_user_university", columnList = "user_id, university_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class UniversityMembership extends Auditable {

    @EmbeddedId
    private UniversityMembershipId id;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("universityId")
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    // ========== ROLE RELATIONSHIP ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private UniversityRole role;

    // ========== STATUS & VERIFICATION ==========
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ========== VERIFICATION DETAILS ==========
    @Column(name = "verified_email")
    private String verifiedEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method")
    private VerificationMethod verificationMethod;

    @Column(name = "verification_date")
    private Instant verificationDate;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private Instant verificationTokenExpiry;

    // ========== ACADEMIC INFO ==========
    @Column(name = "student_id")
    private String studentId;

    @Column(name = "major")
    private String major;

    @Column(name = "faculty")
    private String faculty;

    @Column(name = "enrollment_year")
    private Integer enrollmentYear;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "expected_graduation")
    private Integer expectedGraduation;

    // ========== STATISTICS ==========
    @Column(name = "topic_count", nullable = false)
    private Integer topicCount = 0;

    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0;

    @Column(name = "helpful_votes", nullable = false)
    private Integer helpfulVotes = 0;

    @Column(name = "solution_count", nullable = false)
    private Integer solutionCount = 0;

    @Column(name = "badge_display_order")
    private Integer badgeDisplayOrder = 0;

    // ========== CONSTRUCTORS ==========
    public UniversityMembership(User user, University university, UniversityRole role) {
        this.id = new UniversityMembershipId(user.getUserId(), university.getUniversityId());
        this.user = user;
        this.university = university;
        this.role = role;
    }

    // ========== EQUALS & HASHCODE ==========
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniversityMembership that = (UniversityMembership) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ========== EMBEDDED ID CLASS ==========
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniversityMembershipId implements Serializable {
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "university_id")
        private Integer universityId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UniversityMembershipId that = (UniversityMembershipId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(universityId, that.universityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, universityId);
        }
    }
}
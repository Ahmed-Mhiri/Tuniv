package com.tuniv.backend.university.model;

import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "university_memberships")
@Getter
@Setter
public class UniversityMembership {

    @EmbeddedId
    private UniversityMembershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("universityId")
    private University university;

    @Column(nullable = false)
    private String role; // e.g., "student", "professor"

    // Composite Key Class
    @Embeddable
    @Getter
    @Setter
    public static class UniversityMembershipId implements Serializable {
        private Integer userId;
        private Integer universityId;

        // equals and hashCode methods are essential for composite keys
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
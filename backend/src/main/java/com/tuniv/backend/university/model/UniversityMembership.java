package com.tuniv.backend.university.model;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "university_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "university_id"})
})
@Getter
@Setter
public class UniversityMembership {

    @EmbeddedId
    private UniversityMembershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonBackReference("user-memberships")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("universityId")
    @JoinColumn(name = "university_id")
    @JsonBackReference("university-members")
    private University university;

    @Column(nullable = false)
    private String role;

    @Embeddable
    @Getter
    @Setter
    public static class UniversityMembershipId implements Serializable {
        
        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "university_id")
        private Integer universityId;

        public UniversityMembershipId() {}

        public UniversityMembershipId(Integer userId, Integer universityId) {
            this.userId = userId;
            this.universityId = universityId;
        }

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
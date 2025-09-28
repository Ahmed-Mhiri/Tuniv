package com.tuniv.backend.community.model;

import java.io.Serializable;

import com.tuniv.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_memberships")
@Getter
@Setter
public class CommunityMembership {

    @EmbeddedId
    private CommunityMembershipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("communityId")
    @JoinColumn(name = "community_id")
    private Community community;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityRole role = CommunityRole.MEMBER;

    // Embedded ID Class
    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class CommunityMembershipId implements Serializable {
        private Integer userId;
        private Integer communityId;

        // Helper constructor for easy instantiation
        public CommunityMembershipId(Integer userId, Integer communityId) {
            this.userId = userId;
            this.communityId = communityId;
        }
    }
}
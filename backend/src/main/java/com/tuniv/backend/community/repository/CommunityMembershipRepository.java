package com.tuniv.backend.community.repository;



import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.CommunityMembership;

@Repository
public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, CommunityMembership.CommunityMembershipId> {
    
    /**
     * Finds a specific membership entry using its composite key parts.
     */
    Optional<CommunityMembership> findById_UserIdAndId_CommunityId(Integer userId, Integer communityId);

    /**
     * Checks if a user is a member of a specific community.
     * This is more efficient than fetching the entire membership object.
     */
    boolean existsById_UserIdAndId_CommunityId(Integer userId, Integer communityId);

    /**
     * Finds all community memberships for a given user, pre-fetching community details.
     */
    @EntityGraph(attributePaths = {"community"})
    List<CommunityMembership> findById_UserId(Integer userId);

    /**
     * ✅ ADDED: Finds all members of a given community, pre-fetching user details.
     * This is essential for displaying a community's member list.
     */
    @EntityGraph(attributePaths = {"user"})
    List<CommunityMembership> findById_CommunityId(Integer communityId);

    /**
     * ✅ ADDED: A paginated version for displaying large member lists efficiently.
     */
    @EntityGraph(attributePaths = {"user"})
    Page<CommunityMembership> findById_CommunityId(Integer communityId, Pageable pageable);

    /**
     * ✅ ADDED: Finds all members in a community who have a specific role (e.g., all moderators).
     */
    @EntityGraph(attributePaths = {"user"})
    List<CommunityMembership> findById_CommunityIdAndRole(Integer communityId, CommunityRole role);

    /**
     * ✅ ADDED: Finds all banned members in a community, for moderation purposes.
     */
    @EntityGraph(attributePaths = {"user"})
    List<CommunityMembership> findById_CommunityIdAndIsBannedTrue(Integer communityId);

    /**
     * ✅ ADDED: Atomically updates the contribution score for a member.
     * Crucial for maintaining denormalized user stats within the community.
     */
    @Modifying
    @Query("""
        UPDATE CommunityMembership cm 
        SET cm.contributionScore = cm.contributionScore + :amount 
        WHERE cm.id.userId = :userId AND cm.id.communityId = :communityId
    """)
    void updateContributionScore(
        @Param("userId") Integer userId, 
        @Param("communityId") Integer communityId, 
        @Param("amount") int amount
    );
}
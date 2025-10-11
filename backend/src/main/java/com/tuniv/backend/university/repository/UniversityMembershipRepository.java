package com.tuniv.backend.university.repository;

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

import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.model.VerificationStatus;

@Repository
public interface UniversityMembershipRepository 
    extends JpaRepository<UniversityMembership, UniversityMembership.UniversityMembershipId> {

    /**
     * Finds a membership by its composite key parts.
     */
    Optional<UniversityMembership> findById_UserIdAndId_UniversityId(Integer userId, Integer universityId);

    /**
     * Finds all university memberships for a specific user.
     */
    @EntityGraph(attributePaths = {"university"})
    List<UniversityMembership> findById_UserId(Integer userId);
    
    /**
     * Finds the primary university membership for a user.
     */
    @EntityGraph(attributePaths = {"university"})
    Optional<UniversityMembership> findById_UserIdAndIsPrimaryTrue(Integer userId);

    /**
     * Checks for existence using the composite key parts.
     */
    boolean existsById_UserIdAndId_UniversityId(Integer userId, Integer universityId);

    /**
     * Finds a membership using the verification token, for email confirmation.
     */
    Optional<UniversityMembership> findByVerificationToken(String token);

    /**
     * Finds all memberships for a university with a specific status.
     * Useful for admin tasks like approving pending members.
     */
    List<UniversityMembership> findById_UniversityIdAndStatus(Integer universityId, VerificationStatus status);

    /**
     * ✅ ADDED: A paginated version for admin UIs that may handle many users.
     */
    @EntityGraph(attributePaths = {"user"})
    Page<UniversityMembership> findById_UniversityIdAndStatus(Integer universityId, VerificationStatus status, Pageable pageable);

    /**
     * ✅ ADDED: An efficient way to count members without fetching the full list.
     */
    long countById_UniversityIdAndStatus(Integer universityId, VerificationStatus status);
    
    /**
     * ✅ ADDED: Atomically increments the topic count for a specific membership.
     * This is crucial for maintaining the denormalized user statistics.
     */
    @Modifying
    @Query("UPDATE UniversityMembership um SET um.topicCount = um.topicCount + 1 WHERE um.id.userId = :userId AND um.id.universityId = :universityId")
    void incrementTopicCount(@Param("userId") Integer userId, @Param("universityId") Integer universityId);
    
    /**
     * ✅ ADDED: Atomically increments the reply count for a specific membership.
     */
    @Modifying
    @Query("UPDATE UniversityMembership um SET um.replyCount = um.replyCount + 1 WHERE um.id.userId = :userId AND um.id.universityId = :universityId")
    void incrementReplyCount(@Param("userId") Integer userId, @Param("universityId") Integer universityId);
}
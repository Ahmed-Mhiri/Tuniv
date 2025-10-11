package com.tuniv.backend.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.dto.CommunityDto;

@Repository
public interface UniversityRepository extends JpaRepository<University, Integer>, JpaSpecificationExecutor<University> {

    /**
     * Finds the top universities based on the denormalized member count.
     */
    @Query("SELECT u FROM University u ORDER BY u.memberCount DESC")
    List<University> findTopByOrderByMemberCountDesc(Pageable pageable);

    /**
     * Finds all universities a specific user is a member of.
     */
    @Query("SELECT u FROM University u JOIN UniversityMembership um ON u.universityId = um.id.universityId WHERE um.id.userId = :userId")
    List<University> findUniversitiesByUserId(@Param("userId") Integer userId);

    /**
     * Finds a university by its unique email domain.
     */
    Optional<University> findByEmailDomain(String emailDomain);
    
    /**
     * Finds a university by its unique name (case-insensitive).
     */
    Optional<University> findByNameIgnoreCase(String name);

    /**
     * ✅ ADDED: Provides a paginated search for universities by name.
     * Ideal for a public-facing university search page.
     */
    Page<University> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * ✅ ADDED: Finds the most active universities based on topic count.
     */
    @Query("SELECT u FROM University u ORDER BY u.topicCount DESC")
    List<University> findTopByOrderByTopicCountDesc(Pageable pageable);

    /**
     * Atomically increments the member count for a university.
     */
    @Modifying
    @Query("UPDATE University u SET u.memberCount = u.memberCount + 1 WHERE u.universityId = :universityId")
    void incrementMemberCount(@Param("universityId") Integer universityId);
    
    /**
     * ✅ ADDED: Atomically decrements the member count.
     * Should be called when a user's membership is removed.
     */
    @Modifying
    @Query("UPDATE University u SET u.memberCount = u.memberCount - 1 WHERE u.universityId = :universityId AND u.memberCount > 0")
    void decrementMemberCount(@Param("universityId") Integer universityId);

    /**
     * Atomically increments the topic count for a university.
     */
    @Modifying
    @Query("UPDATE University u SET u.topicCount = u.topicCount + 1 WHERE u.universityId = :universityId")
    void incrementTopicCount(@Param("universityId") Integer universityId);

    /**
     * ✅ ADDED: Atomically decrements the topic count.
     * Should be called when a topic within the university is deleted.
     */
    @Modifying
    @Query("UPDATE University u SET u.topicCount = u.topicCount - 1 WHERE u.universityId = :universityId AND u.topicCount > 0")
    void decrementTopicCount(@Param("universityId") Integer universityId);
}
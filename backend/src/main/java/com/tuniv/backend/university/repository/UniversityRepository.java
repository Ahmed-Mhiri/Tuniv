package com.tuniv.backend.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.University;
import com.tuniv.backend.user.dto.CommunityDto;

@Repository
public interface UniversityRepository extends JpaRepository<University, Integer>, JpaSpecificationExecutor<University> {

    @Override
    @EntityGraph(attributePaths = {"modules", "memberships"})
    Page<University> findAll(Specification<University> spec, Pageable pageable);

    @Query("SELECT u FROM University u ORDER BY SIZE(u.memberships) DESC LIMIT 5")
    List<University> findTop5ByOrderByMembershipsSizeDesc();

    @Query("""
        SELECT NEW com.tuniv.backend.user.dto.CommunityDto(
            u.id,
            'UNIVERSITY',
            u.name,
            (SELECT COUNT(m.id.userId) FROM UniversityMembership m WHERE m.university.id = u.id),
            (SELECT COUNT(q.id) FROM Question q WHERE q.module.university.id = u.id)
        )
        FROM University u
        JOIN u.memberships um
        WHERE um.user.id = :userId
        GROUP BY u.id, u.name
    """)
    List<CommunityDto> findUserCommunitiesWithStats(@Param("userId") Integer userId);

    Optional<University> findByEmailDomain(String emailDomain);

}
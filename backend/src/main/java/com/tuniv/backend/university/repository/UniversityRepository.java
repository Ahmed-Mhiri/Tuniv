package com.tuniv.backend.university.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.University;

@Repository
public interface UniversityRepository extends JpaRepository<University, Integer>, JpaSpecificationExecutor<University> {
@Override
    @EntityGraph(attributePaths = {"modules", "memberships"})
    Page<University> findAll(Specification<University> spec, Pageable pageable);

    // ✅ ADD THIS NEW METHOD
    @Query("SELECT u FROM University u ORDER BY SIZE(u.memberships) DESC LIMIT 5")
    List<University> findTop5ByOrderByMembershipsSizeDesc();
}
package com.tuniv.backend.university.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.Module;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    // ✅ You need this method for the NON-PAGINATED list
    List<Module> findByUniversityUniversityId(Integer universityId);
    
    // Spring Data JPA will automatically handle pagination for this method
    Page<Module> findByUniversityUniversityId(Integer universityId, Pageable pageable);
}
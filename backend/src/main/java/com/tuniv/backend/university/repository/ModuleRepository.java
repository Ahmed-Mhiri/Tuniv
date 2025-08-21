package com.tuniv.backend.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.Module;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    // Custom query to find all modules for a given university ID
    List<Module> findByUniversityUniversityId(Integer universityId);
}
package com.tuniv.backend.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.Module;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    
    /**
     * Finds all modules for a given university, without pagination.
     */
    List<Module> findByUniversity_UniversityId(Integer universityId);
    
    /**
     * Finds a paginated list of modules for a given university.
     */
    Page<Module> findByUniversity_UniversityId(Integer universityId, Pageable pageable);

    /**
     * Finds a specific module within a university by its code (case-insensitive).
     * Useful for direct lookups, e.g., /universities/1/modules/CS101.
     */
    Optional<Module> findByUniversity_UniversityIdAndCodeIgnoreCase(Integer universityId, String code);

    /**
     * ✅ ADDED: Provides search functionality for modules within a specific university.
     * Ideal for a search bar on a university's module list page.
     */
    Page<Module> findByUniversity_UniversityIdAndNameContainingIgnoreCase(Integer universityId, String name, Pageable pageable);

    /**
     * ✅ ADDED: Finds the most popular modules in a university based on topic count.
     *
     * @param universityId The ID of the university.
     * @param pageable Defines how many modules to fetch (e.g., PageRequest.of(0, 5) for the top 5).
     * @return A list of the most active modules.
     */
    List<Module> findByUniversity_UniversityIdOrderByTopicCountDesc(Integer universityId, Pageable pageable);

    /**
     * Atomically increments the topic count for a module.
     * This is crucial for keeping your denormalized statistics accurate.
     */
    @Modifying
    @Query("UPDATE Module m SET m.topicCount = m.topicCount + 1 WHERE m.moduleId = :moduleId")
    void incrementTopicCount(@Param("moduleId") Integer moduleId);

    /**
     * ✅ ADDED: Atomically decrements the topic count for a module.
     * This should be called whenever a topic is deleted from a module.
     */
    @Modifying
    @Query("UPDATE Module m SET m.topicCount = m.topicCount - 1 WHERE m.moduleId = :moduleId AND m.topicCount > 0")
    void decrementTopicCount(@Param("moduleId") Integer moduleId);
}
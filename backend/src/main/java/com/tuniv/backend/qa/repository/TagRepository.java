package com.tuniv.backend.qa.repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Tag;
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    /**
     * Finds a tag by its unique name (case-insensitive).
     */
    Optional<Tag> findByNameIgnoreCase(String name);

    /**
     * Efficiently finds all existing tags from a given list of names (case-insensitive).
     */
    List<Tag> findByNameIgnoreCaseIn(Collection<String> names);

    /**
     * ✅ ADD: Finds the most popular tags based on their denormalized usage count.
     * This is very fast due to the index on the `usage_count` column.
     *
     * @param pageable Defines how many tags to fetch (e.g., PageRequest.of(0, 10) for the top 10).
     * @return A list of the most used tags.
     */
    List<Tag> findByOrderByUsageCountDesc(Pageable pageable);

    /**
     * ✅ ADD: Implements a search functionality for tags, ideal for autocomplete features.
     *
     * @param name The partial name of the tag to search for.
     * @param pageable Pagination information.
     * @return A paginated list of matching tags.
     */
    Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * ✅ ADD: Atomically increments the usage count and updates the last used timestamp for a given tag.
     * This is the correct way to maintain denormalized data.
     */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1, t.lastUsedAt = CURRENT_TIMESTAMP WHERE t.id = :tagId")
    void incrementUsageCount(@Param("tagId") Integer tagId);
    
    /**
     * ✅ ADD: Atomically decrements the usage count for a given tag.
     */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount - 1 WHERE t.id = :tagId AND t.usageCount > 0")
    void decrementUsageCount(@Param("tagId") Integer tagId);
}

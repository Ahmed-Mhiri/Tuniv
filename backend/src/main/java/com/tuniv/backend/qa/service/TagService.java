package com.tuniv.backend.qa.service;

import com.tuniv.backend.follow.model.Followable;
import com.tuniv.backend.follow.model.FollowableType;
import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Tags.
 * Handles the creation of new tags and finding existing ones.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * Finds existing tags by name and creates new ones for any names not found.
     * This method is idempotent and ensures proper name normalization.
     *
     * @param tagNames A list of strings representing the tag names.
     * @return A Set of managed Tag entities.
     */
    @Transactional
    public Set<Tag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        // 1. ✅ NORMALIZE input names to match the entity's logic BEFORE querying.
        Set<String> normalizedNames = tagNames.stream()
            .map(this::normalizeTagName)
            .collect(Collectors.toSet());

        // 2. Find all tags that already exist in the database in one query.
        List<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Set<String> existingNames = existingTags.stream()
            .map(Tag::getName)
            .collect(Collectors.toSet());

        // 3. Determine which normalized tag names are new.
        Set<String> newNamesToCreate = normalizedNames.stream()
            .filter(name -> !existingNames.contains(name))
            .collect(Collectors.toSet());
            
        if (newNamesToCreate.isEmpty()) {
            return new HashSet<>(existingTags);
        }

        // 4. ✅ CORRECTLY CREATE new tags with their required `Followable` association.
        List<Tag> newTags = newNamesToCreate.stream()
            .map(name -> {
                // Every new tag MUST have a corresponding Followable entity.
                Followable followable = new Followable(FollowableType.TAG);
                
                // Use the constructor that handles normalization.
                Tag newTag = new Tag(name, null); // Description can be null
                newTag.setFollowable(followable);
                return newTag;
            })
            .collect(Collectors.toList());

        // 5. Save all new tags in one batch.
        tagRepository.saveAll(newTags);

        // 6. Combine existing tags and newly created tags for the final result.
        Set<Tag> allTags = new HashSet<>(existingTags);
        allTags.addAll(newTags);
        return allTags;
    }

    /**
     * Finds a list of tags by their names.
     *
     * @param tagNames The list of names to search for.
     * @return A List of found Tag entities.
     */
    @Transactional(readOnly = true)
    public List<Tag> getTagsByNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        Set<String> normalizedNames = tagNames.stream()
            .map(this::normalizeTagName)
            .collect(Collectors.toSet());
        return tagRepository.findByNameIn(normalizedNames);
    }
    
    /**
     * Helper method to normalize a tag name, mirroring the logic in the Tag entity.
     * This ensures consistency between what's in the database and what we're searching for.
     *
     * @param name The raw tag name.
     * @return A normalized string (lowercase, trimmed, spaces replaced with hyphens).
     */
    private String normalizeTagName(String name) {
        if (name == null) return null;
        return name.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}
package com.tuniv.backend.qa.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.qa.model.Tag;
import com.tuniv.backend.qa.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * Finds existing tags and creates new ones for any names not found.
     * @param tagNames A list of strings representing the tag names.
     * @return A Set of managed Tag entities.
     */
    @Transactional
    public Set<Tag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        // Sanitize input: lowercase and distinct
        Set<String> distinctNames = tagNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Find all tags that already exist in the database in one query
        List<Tag> existingTags = tagRepository.findByNameIn(distinctNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        // Determine which tag names are new
        Set<Tag> newTags = distinctNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return newTag;
                })
                .collect(Collectors.toSet());

        // Save all new tags in one go
        tagRepository.saveAll(newTags);

        // Combine existing tags and newly created tags
        Set<Tag> allTags = new HashSet<>(existingTags);
        allTags.addAll(newTags);
        return allTags;
    }
}

package com.tuniv.backend.qa.repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Tag;
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    
    // Efficiently finds all existing tags from a given list of names
    List<Tag> findByNameIn(Collection<String> names);
    
    Optional<Tag> findByName(String name);
}

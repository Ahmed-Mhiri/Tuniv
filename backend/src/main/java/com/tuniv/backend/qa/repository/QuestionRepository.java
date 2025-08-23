package com.tuniv.backend.qa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // <-- IMPORT ADDED
import org.springframework.data.jpa.repository.JpaRepository; // <-- IMPORT ADDED
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    // --- METHOD SIGNATURE CHANGED ---
    // It now accepts a Pageable object and returns a Page of Questions
    Page<Question> findByModuleModuleId(Integer moduleId, Pageable pageable);
}
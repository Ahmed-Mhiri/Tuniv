package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByModuleModuleId(Integer moduleId);
}
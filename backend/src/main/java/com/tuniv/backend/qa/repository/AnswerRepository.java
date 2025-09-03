package com.tuniv.backend.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.qa.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    List<Answer> findByAuthorUserIdOrderByCreatedAtDesc(Integer userId);
List<Answer> findByQuestionAuthorUserIdAndIsSolutionTrueOrderByUpdatedAtDesc(Integer userId);
}
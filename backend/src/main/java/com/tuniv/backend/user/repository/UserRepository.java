package com.tuniv.backend.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetPasswordToken(String token);
    Optional<User> findByVerificationToken(String token);
    @Query("SELECT m.user FROM UniversityMembership m WHERE m.university.universityId = :universityId AND m.user.userId != :authorId")
    List<User> findAllMembersOfUniversityExcludingAuthor(Integer universityId, Integer authorId);
}
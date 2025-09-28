package com.tuniv.backend.follow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.follow.model.Follow;
import com.tuniv.backend.follow.model.FollowableType;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {

    // Find all things a user follows
    List<Follow> findAllByUser_UserId(Integer userId);

    // Find followers for a specific target
    List<Follow> findByTargetTypeAndTargetId(FollowableType targetType, Integer targetId);

    // Find specific follow relationship
    Optional<Follow> findByUser_UserIdAndTargetTypeAndTargetId(Integer userId, FollowableType targetType, Integer targetId);

    // Count how many followers a specific target has
    long countByTargetTypeAndTargetId(FollowableType targetType, Integer targetId);
    
    // Check if a user already follows a specific target
    boolean existsByUser_UserIdAndTargetTypeAndTargetId(Integer userId, FollowableType targetType, Integer targetId);

}

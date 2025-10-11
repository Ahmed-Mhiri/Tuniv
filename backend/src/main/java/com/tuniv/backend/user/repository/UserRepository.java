package com.tuniv.backend.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find multiple users by their IDs
     */
    List<User> findByUserIdIn(List<Integer> userIds);
    
    /**
     * Find multiple users by their IDs and return as Set
     */
    @Query("SELECT u FROM User u WHERE u.userId IN :userIds")
    Set<User> findAllByUserIdIn(@Param("userIds") List<Integer> userIds);
    
    /**
     * Find active users by their IDs
     */
    @Query("SELECT u FROM User u WHERE u.userId IN :userIds AND u.isEnabled = true AND u.isDeleted = false")
    List<User> findActiveUsersByIds(@Param("userIds") List<Integer> userIds);
    
    /**
     * Check if all user IDs exist and are active
     */
    @Query("SELECT COUNT(u) = :expectedCount FROM User u WHERE u.userId IN :userIds AND u.isEnabled = true AND u.isDeleted = false")
    boolean allUsersExistAndActive(@Param("userIds") List<Integer> userIds, 
                                 @Param("expectedCount") long expectedCount);
    
    /**
     * Find users by username pattern
     */
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    /**
     * Find users by email domain
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain")
    List<User> findByEmailDomain(@Param("domain") String domain);
    
    /**
     * Count active users
     */
    long countByIsEnabledTrueAndIsDeletedFalse();
    
    /**
     * Find users with high reputation
     */
    @Query("SELECT u FROM User u WHERE u.reputationScore >= :minReputation AND u.isEnabled = true AND u.isDeleted = false ORDER BY u.reputationScore DESC")
    List<User> findTopUsersByReputation(@Param("minReputation") Integer minReputation, 
                                      org.springframework.data.domain.Pageable pageable);

}
package com.tuniv.backend.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.UniversityMembership;

@Repository
public interface UniversityMembershipRepository 
    extends JpaRepository<UniversityMembership, UniversityMembership.UniversityMembershipId> {
    @Modifying
    @Query("DELETE FROM UniversityMembership m WHERE m.user.userId = :userId AND m.university.universityId = :universityId")
    void deleteByUserIdAndUniversityId(@Param("userId") Integer userId, @Param("universityId") Integer universityId);
     boolean existsByUser_UserIdAndUniversity_UniversityId(Integer userId, Integer universityId);

}
package com.tuniv.backend.community.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.CommunityMembership;

@Repository
public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, CommunityMembership.CommunityMembershipId> {
    
    // Find all community memberships for a given user
    List<CommunityMembership> findByUser_UserId(Integer userId);

}

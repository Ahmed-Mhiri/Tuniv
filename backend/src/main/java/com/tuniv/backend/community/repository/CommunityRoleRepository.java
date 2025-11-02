package com.tuniv.backend.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.community.model.CommunityRole;

@Repository
public interface CommunityRoleRepository extends JpaRepository<CommunityRole, Integer> {
    Optional<CommunityRole> findByNameAndIsSystemRole(String name, boolean isSystemRole);
    List<CommunityRole> findByCommunity_CommunityId(Integer communityId);
}
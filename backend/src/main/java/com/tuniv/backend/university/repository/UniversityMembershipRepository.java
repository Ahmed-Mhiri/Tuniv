package com.tuniv.backend.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.university.model.UniversityMembership;

@Repository
public interface UniversityMembershipRepository 
    extends JpaRepository<UniversityMembership, UniversityMembership.UniversityMembershipId> {
}
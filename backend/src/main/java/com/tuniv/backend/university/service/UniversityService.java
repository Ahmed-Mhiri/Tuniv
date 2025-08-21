package com.tuniv.backend.university.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.model.UniversityMembership;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final UniversityMembershipRepository membershipRepository;

    public List<UniversityDto> getAllUniversities() {
        return universityRepository.findAll().stream()
                .map(this::mapUniversityToDto)
                .collect(Collectors.toList());
    }

    public List<ModuleDto> getModulesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        return moduleRepository.findByUniversityUniversityId(universityId).stream()
                .map(this::mapModuleToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinUniversity(Integer universityId, UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));
        
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found with id: " + universityId));

        UniversityMembership.UniversityMembershipId membershipId = new UniversityMembership.UniversityMembershipId();
        membershipId.setUserId(user.getUserId());
        membershipId.setUniversityId(university.getUniversityId());

        if (membershipRepository.existsById(membershipId)) {
            throw new IllegalArgumentException("User is already a member of this university.");
        }

        UniversityMembership membership = new UniversityMembership();
        membership.setId(membershipId);
        membership.setUser(user);
        membership.setUniversity(university);
        membership.setRole("student");

        membershipRepository.save(membership);
    }
    
    private UniversityDto mapUniversityToDto(University university) {
        List<ModuleDto> moduleDtos = university.getModules().stream()
                .map(this::mapModuleToDto)
                .collect(Collectors.toList());
        return new UniversityDto(university.getUniversityId(), university.getName(), moduleDtos);
    }

    private ModuleDto mapModuleToDto(Module module) {
        return new ModuleDto(module.getModuleId(), module.getName());
    }
}
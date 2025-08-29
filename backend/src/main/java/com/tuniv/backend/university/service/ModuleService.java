package com.tuniv.backend.university.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import com.tuniv.backend.university.repository.UniversityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UniversityRepository universityRepository;
    private final UniversityMembershipRepository membershipRepository;


    @Transactional(readOnly = true)
    public ModuleDetailDto getModuleDetails(Integer moduleId, UserDetailsImpl currentUser) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        
        University parentUniversity = module.getUniversity();

        // ✅ FIX: Call the new, correctly named method from the repository
        boolean isMember = membershipRepository.existsByUser_UserIdAndUniversity_UniversityId(
            currentUser.getId(), 
            parentUniversity.getUniversityId()
        );
        
        return UniversityMapper.toModuleDetailDto(module, isMember);
    }


    @Transactional(readOnly = true)
    public List<ModuleDto> getModulesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        return moduleRepository.findByUniversityUniversityId(universityId).stream()
                .map(UniversityMapper::toModuleDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModules() {
        return moduleRepository.findAll().stream()
                .map(UniversityMapper::toModuleDto)
                .collect(Collectors.toList());
    }
}
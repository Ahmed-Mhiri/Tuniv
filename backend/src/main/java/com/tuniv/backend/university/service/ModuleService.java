package com.tuniv.backend.university.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<ModuleDto> getModulesByUniversity(Integer universityId, Pageable pageable) {
        if (!universityRepository.existsById(universityId)) {
            throw new ResourceNotFoundException("University not found with id: " + universityId);
        }
        // The repository call now returns a Page<Module>
        Page<Module> modulePage = moduleRepository.findByUniversityUniversityId(universityId, pageable);
        
        // Use the map function provided by the Page interface to convert Page<Module> to Page<ModuleDto>
        return modulePage.map(UniversityMapper::toModuleDto);
    }

    @Transactional(readOnly = true)
    public Page<ModuleDto> getAllModules(Pageable pageable) {
        // The findAll method from JpaRepository already supports Pageable
        return moduleRepository.findAll(pageable)
                               .map(UniversityMapper::toModuleDto);
    }
    @Transactional(readOnly = true)
public List<ModuleDto> getAllModulesByUniversity(Integer universityId) {
    if (!universityRepository.existsById(universityId)) {
        throw new ResourceNotFoundException("University not found with id: " + universityId);
    }
    
    // This calls the repository method that returns a simple List, not a Page
    return moduleRepository.findByUniversityUniversityId(universityId).stream()
           .map(UniversityMapper::toModuleDto)
           .collect(Collectors.toList());
}
}
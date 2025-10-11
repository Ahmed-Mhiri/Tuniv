package com.tuniv.backend.university.service;

import com.tuniv.backend.config.security.services.UserDetailsImpl;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.mapper.UniversityMapper;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.repository.ModuleRepository;
import com.tuniv.backend.university.repository.UniversityMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final UniversityMembershipRepository membershipRepository;
    private final UniversityMapper universityMapper; // ✅ INJECTED MAPPER

    @Transactional(readOnly = true)
    public ModuleDetailDto getModuleDetails(Integer moduleId, UserDetailsImpl currentUser) {
        Module module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        // The logic to check membership remains efficient and correct.
        boolean isMember = (currentUser != null) && membershipRepository.existsByUser_UserIdAndUniversity_UniversityId(
            currentUser.getId(),
            module.getUniversity().getUniversityId()
        );

        return universityMapper.toModuleDetailDto(module, isMember);
    }

    @Transactional(readOnly = true)
    public Page<ModuleDto> getModulesByUniversity(Integer universityId, Pageable pageable, UserDetailsImpl currentUser) {
        boolean isMember = (currentUser != null) && membershipRepository.existsByUser_UserIdAndUniversity_UniversityId(
            currentUser.getId(),
            universityId
        );
        
        Page<Module> modulePage = moduleRepository.findByUniversityUniversityId(universityId, pageable);
        return modulePage.map(module -> universityMapper.toModuleDto(module, isMember));
    }
}
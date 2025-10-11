package com.tuniv.backend.university.mapper;

import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityBasicDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps University and Module entities to their corresponding DTOs.
 * Follows a component-based approach for dependency injection.
 */
@Component
public class UniversityMapper {

    /**
     * ✅ REFACTORED: Maps a University and a separately provided list of its modules to a UniversityDto.
     * This decouples the mapper from the university entity's structure, avoiding N+1 problems.
     */
    public UniversityDto toUniversityDto(University university, boolean isMember, List<ModuleDto> moduleDtos) {
        if (university == null) {
            return null;
        }

        return new UniversityDto(
            university.getUniversityId(),
            university.getName(),
            moduleDtos, // ✅ Use the provided list
            isMember,
            university.getMemberCount(),
            university.getTopicCount()
        );
    }

    public UniversityBasicDto toUniversityBasicDto(University university, boolean isMember) {
        if (university == null) {
            return null;
        }
        return new UniversityBasicDto(
            university.getUniversityId(),
            university.getName(),
            isMember,
            university.getTopicCount(),
            university.getMemberCount()
        );
    }
    
    public ModuleDto toModuleDto(Module module, boolean isMemberOfParentUniversity) {
        if (module == null) {
            return null;
        }
        return new ModuleDto(
            module.getModuleId(),
            module.getName(),
            module.getTopicCount(),
            isMemberOfParentUniversity
        );
    }
    
    public ModuleDetailDto toModuleDetailDto(Module module, boolean isMemberOfParentUniversity) {
        if (module == null) {
            return null;
        }
        UniversityBasicDto universityDto = toUniversityBasicDto(module.getUniversity(), isMemberOfParentUniversity);
        return new ModuleDetailDto(
            module.getModuleId(),
            module.getName(),
            universityDto,
            module.getTopicCount(),
            isMemberOfParentUniversity
        );
    }
}
package com.tuniv.backend.university.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityBasicDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;

public final class UniversityMapper {

    private UniversityMapper() {
        // Private constructor to prevent instantiation
    }

    public static UniversityDto toUniversityDto(University university, boolean isMember) {
        if (university == null) {
            return null;
        }

        return new UniversityDto(
            university.getUniversityId(),
            university.getName(),
            university.getModules() != null ?
                university.getModules().stream()
                    .map(module -> UniversityMapper.toModuleDto(module, isMember)) // ✅ Pass isMember to modules
                    .collect(Collectors.toList()) :
                Collections.emptyList(),
            isMember,
            university.getMemberCount(),  // ✅ Use stored denormalized count
            university.getQuestionCount() // ✅ Use stored denormalized count
        );
    }

    public static UniversityDto toUniversityDto(University university) {
        return toUniversityDto(university, false);
    }

    public static ModuleDto toModuleDto(Module module) {
        if (module == null) {
            return null;
        }
        return new ModuleDto(
            module.getModuleId(),
            module.getName(),
            module.getQuestionCount(), // ✅ Use stored denormalized count
            false // Default value, will be set in service
        );
    }

    public static ModuleDto toModuleDto(Module module, boolean isMember) {
        if (module == null) {
            return null;
        }
        return new ModuleDto(
            module.getModuleId(),
            module.getName(),
            module.getQuestionCount(), // ✅ Use stored denormalized count
            isMember // ✅ Added member status
        );
    }
    
    public static ModuleDetailDto toModuleDetailDto(Module module, boolean isMember) {
        if (module == null) {
            return null;
        }

        UniversityBasicDto universityDto = new UniversityBasicDto(
            module.getUniversity().getUniversityId(),
            module.getUniversity().getName(),
            isMember,
            module.getUniversity().getQuestionCount(), // ✅ Use stored denormalized count
            module.getUniversity().getMemberCount()    // ✅ Use stored denormalized count
        );

        return new ModuleDetailDto(
            module.getModuleId(),
            module.getName(),
            universityDto,
            module.getQuestionCount(), // ✅ Use stored denormalized count
            isMember // ✅ Module member status
        );
    }

    public static UniversityBasicDto toUniversityBasicDto(University university, boolean isMember) {
        if (university == null) {
            return null;
        }
        
        return new UniversityBasicDto(
            university.getUniversityId(),
            university.getName(),
            isMember,
            university.getQuestionCount(), // ✅ Use stored denormalized count
            university.getMemberCount()    // ✅ Use stored denormalized count
        );
    }
}
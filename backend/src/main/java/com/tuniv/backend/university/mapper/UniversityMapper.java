package com.tuniv.backend.university.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.tuniv.backend.university.dto.ModuleDetailDto;
import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityBasicDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;

public final class UniversityMapper { // Use 'final' for utility classes with only static methods

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
                .map(UniversityMapper::toModuleDto)
                .collect(Collectors.toList()) :
            Collections.emptyList(),
        isMember,
        // ✅ ADD THIS LINE: Calculate the size of the members set
        university.getMembers() != null ? university.getMembers().size() : 0
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
            module.getName()
        );
    }
    
    public static ModuleDetailDto toModuleDetailDto(Module module, boolean isMember) {
        if (module == null) {
            return null;
        }

        // Create the nested University DTO, now including the isMember flag
        UniversityBasicDto universityDto = new UniversityBasicDto(
            module.getUniversity().getUniversityId(),
            module.getUniversity().getName(),
            isMember // <-- Pass the calculated status here
        );

        // Create the main Module Detail DTO
        return new ModuleDetailDto(
            module.getModuleId(),
            module.getName(),
            universityDto
        );
    }
}
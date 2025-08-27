package com.tuniv.backend.university.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;

public class UniversityMapper {

    /**
     * --- NEW: The primary mapping method ---
     * Maps a University entity to a DTO, including the user's membership status.
     * This is the method your updated UniversityService will call.
     */
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
            isMember // <-- FIX: Pass the new 'isMember' flag to the constructor
        );
    }

    /**
     * --- MODIFIED: An overloaded method for convenience ---
     * Maps a University entity to a DTO, defaulting isMember to false.
     * This makes the mapper flexible for other parts of the app that don't need to check membership.
     */
    public static UniversityDto toUniversityDto(University university) {
        return toUniversityDto(university, false);
    }

    /**
     * Maps a Module entity to a DTO. (Unchanged)
     */
    public static ModuleDto toModuleDto(Module module) {
        if (module == null) {
            return null;
        }
        return new ModuleDto(
            module.getModuleId(),
            module.getName()
        );
    }
}
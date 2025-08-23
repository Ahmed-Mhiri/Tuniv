package com.tuniv.backend.university.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tuniv.backend.university.dto.ModuleDto;
import com.tuniv.backend.university.dto.UniversityDto;
import com.tuniv.backend.university.model.Module;
import com.tuniv.backend.university.model.University;

public class UniversityMapper {

    public static UniversityDto toUniversityDto(University university) {
        if (university == null) {
            return null;
        }

        List<ModuleDto> moduleDtos = university.getModules() != null ?
                university.getModules().stream()
                        .map(UniversityMapper::toModuleDto)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return new UniversityDto(
                university.getUniversityId(),
                university.getName(),
                moduleDtos
        );
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
}
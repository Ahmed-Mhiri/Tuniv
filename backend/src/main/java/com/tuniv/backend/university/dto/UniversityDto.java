package com.tuniv.backend.university.dto;

import java.util.List;

public record UniversityDto(
    Integer universityId,
    String name,
    List<ModuleDto> modules,
    boolean isMember, // <-- ADD THIS
    int memberCount

) {}
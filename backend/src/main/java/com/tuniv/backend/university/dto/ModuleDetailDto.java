package com.tuniv.backend.university.dto;

public record ModuleDetailDto(
    Integer moduleId,
    String name,
    UniversityBasicDto university,
    Integer questionCount,
    boolean isMember
) {}
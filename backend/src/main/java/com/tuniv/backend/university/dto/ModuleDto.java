package com.tuniv.backend.university.dto;

public record ModuleDto(
    Integer moduleId,
    String name,
    Integer questionCount,
    boolean isMember
) {}
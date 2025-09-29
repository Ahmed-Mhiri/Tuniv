package com.tuniv.backend.university.dto;

public record ModuleDetailDto(
    Integer moduleId,
    String name,
    UniversityBasicDto university,
    Integer topicCount,  // ✅ UPDATED: questionCount → topicCount
    boolean isMember
) {}
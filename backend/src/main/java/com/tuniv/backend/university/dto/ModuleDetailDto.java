package com.tuniv.backend.university.dto;



// The main DTO for our new endpoint.
public record ModuleDetailDto(
    Integer moduleId,
    String name,
    UniversityBasicDto university
) {}

package com.tuniv.backend.university.dto;

public record UniversityBasicDto(
    Integer universityId,
    String name,
    boolean isMember // <-- ADD THIS FIELD

) {}

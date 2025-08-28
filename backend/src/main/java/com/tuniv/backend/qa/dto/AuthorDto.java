package com.tuniv.backend.qa.dto;

public record AuthorDto(
    Integer userId,
    String username,
    String profilePhotoUrl // <-- ADD THIS FIELD

) {}
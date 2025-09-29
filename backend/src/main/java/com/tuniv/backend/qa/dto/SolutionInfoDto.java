package com.tuniv.backend.qa.dto;

import java.time.Instant;

public record SolutionInfoDto(
    Integer id,
    String body,
    UserDto author,
    Instant createdAt
) {}
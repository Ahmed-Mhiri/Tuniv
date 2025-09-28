package com.tuniv.backend.shared.dto;

public record ApiResponse(
    boolean success,
    String message
) {}

package com.tuniv.backend.qa.dto;

import com.tuniv.backend.shared.model.ContainerType;

public record ContainerInfoDto(
    Integer id,
    String name,
    ContainerType type // "MODULE" or "COMMUNITY"
) {}

package com.tuniv.backend.follow.dto;

import java.util.List;

public record FollowableListResponse(
    // The list of items for the current page
    List<FollowableDto> content,

    // Pagination metadata
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {}

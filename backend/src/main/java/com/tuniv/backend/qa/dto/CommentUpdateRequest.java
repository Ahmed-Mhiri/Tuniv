package com.tuniv.backend.qa.dto;

import java.util.List;

public record CommentUpdateRequest(
    String body,
    List<Integer> attachmentIdsToDelete
) {}

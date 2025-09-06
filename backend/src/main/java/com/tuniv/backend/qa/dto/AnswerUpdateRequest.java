package com.tuniv.backend.qa.dto;

import java.util.List;

public record AnswerUpdateRequest(
    String body,
    List<Integer> attachmentIdsToDelete
) {}

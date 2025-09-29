package com.tuniv.backend.qa.dto;

import java.util.List;


public record ReplyUpdateRequest(
    String body,
    List<Integer> attachmentIdsToDelete
) {}

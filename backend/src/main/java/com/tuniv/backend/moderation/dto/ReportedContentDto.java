package com.tuniv.backend.moderation.dto;

import com.tuniv.backend.shared.model.ContainerType;
import com.tuniv.backend.user.dto.UserSummaryDto;

public record ReportedContentDto(
    Integer contentId,
    ContainerType contentType,
    UserSummaryDto contentAuthor,
    String contentUrl, // A direct link to the reported item
    String contentSnippet // A preview of the content
) {}
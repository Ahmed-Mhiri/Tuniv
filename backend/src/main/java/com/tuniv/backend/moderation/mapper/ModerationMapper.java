package com.tuniv.backend.moderation.mapper;

import com.tuniv.backend.moderation.dto.*;
import com.tuniv.backend.moderation.model.ModerationLog;
import com.tuniv.backend.moderation.model.Report;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.user.dto.UserSummaryDto;
import com.tuniv.backend.user.mapper.UserMapper;
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.chat.model.Message;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ModerationMapper {

    @Mappings({
        @Mapping(source = "report.reportId", target = "reportId"),
        @Mapping(source = "report.reporter.username", target = "reporterUsername"),
        @Mapping(source = "report.targetType", target = "contentType", qualifiedByName = "targetTypeToContainerType"),
    })
    ReportSummaryDto toReportSummaryDto(Report report);

    @Mappings({
        @Mapping(source = "report.reportId", target = "reportId"),
        @Mapping(source = "report.status", target = "status"),
        @Mapping(source = "report.reporter", target = "reporter"), // Uses UserMapper
        @Mapping(source = "report.reason", target = "reason"),
        @Mapping(source = "report.details", target = "reporterComment"),
        @Mapping(source = "report.createdAt", target = "createdAt"),
        @Mapping(source = "report.resolvedBy", target = "assignedModerator"), // Uses UserMapper
        @Mapping(source = "report.resolutionNotes", target = "moderatorNotes"),
        @Mapping(target = "reportedContent", source = "report"), // Custom mapping below
        @Mapping(target = "targetUserHistory", source = "historyLogs") // Manually pass this in
    })
    ReportDetailDto toReportDetailDto(Report report, List<ModerationLogDto> historyLogs);

    // This method maps the polymorphic 'target' to the ReportedContentDto
    default ReportedContentDto mapReportToReportedContentDto(Report report) {
        if (report.getTarget() == null) {
            return null;
        }

        Object target = report.getTarget();
        String contentTypeString = report.getTargetType();
        ContainerType contentType = targetTypeToContainerType(contentTypeString);

        if (contentType == null) {
            return null;
        }

        UserSummaryDto author = null;
        String snippet = null;
        String url = null;
        Integer contentId = null;

        // Populate DTO based on the actual entity type
        switch (contentType) {
            case TOPIC:
            case REPLY:
                Post post = (Post) target;
                author = UserMapper.INSTANCE.userToUserSummaryDto(post.getAuthor());
                snippet = post.getBody().length() > 150 ? post.getBody().substring(0, 150) + "..." : post.getBody();
                url = "/posts/" + post.getId(); // Example URL
                contentId = post.getId();
                break;
            case USER_PROFILE:
                User user = (User) target;
                author = UserMapper.INSTANCE.userToUserSummaryDto(user);
                snippet = user.getBio() != null ? (user.getBio().length() > 150 ? user.getBio().substring(0, 150) + "..." : user.getBio()) : "";
                url = "/users/" + user.getUsername();
                contentId = user.getUserId();
                break;
            case MESSAGE:
                Message message = (Message) target;
                author = UserMapper.INSTANCE.userToUserSummaryDto(message.getAuthor());
                snippet = message.getBody().length() > 150 ? message.getBody().substring(0, 150) + "..." : message.getBody();
                url = "/chat/" + message.getConversation().getConversationId() + "?message=" + message.getId();
                contentId = message.getId();
                break;
            case COMMUNITY:
                Community community = (Community) target;
                author = UserMapper.INSTANCE.userToUserSummaryDto(community.getCreator());
                snippet = community.getDescription() != null ? (community.getDescription().length() > 150 ? community.getDescription().substring(0, 150) + "..." : community.getDescription()) : "";
                url = "/communities/" + community.getName();
                contentId = community.getCommunityId();
                break;
        }
        
        return new ReportedContentDto(contentId, contentType, author, url, snippet);
    }

    @Mappings({
        @Mapping(source = "log.logId", target = "logId"),
        @Mapping(source = "log.moderator", target = "moderator"), // Uses UserMapper
        @Mapping(source = "log.action", target = "action"),
        @Mapping(source = "log.targetUser", target = "targetUser"), // Uses UserMapper
        @Mapping(source = "log.justification", target = "reason"),
        @Mapping(source = "log.createdAt", target = "timestamp"),
        @Mapping(target = "targetContentSummary", source = "log.targetPost") // Custom mapping
    })
    ModerationLogDto toModerationLogDto(ModerationLog log);

    default String mapPostToSummary(Post post) {
        if (post == null) return "N/A";
        return post.getPostType() + " #" + post.getId();
    }

    // Helper to convert DB string to enum
    @Named("targetTypeToContainerType")
    default ContainerType targetTypeToContainerType(String targetType) {
        if (targetType == null) return null;
        try {
            // Your CreateReportRequest uses ContainerType, but Report entity uses strings
            // We need to map between them.
            if ("POST".equals(targetType)) return ContainerType.TOPIC; // Or REPLY, logic needed
            if ("USER".equals(targetType)) return ContainerType.USER_PROFILE;
            if ("MESSAGE".equals(targetType)) return ContainerType.MESSAGE;
            if ("COMMUNITY".equals(targetType)) return ContainerType.COMMUNITY;
            // Fallback for direct enum name
            return ContainerType.valueOf(targetType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
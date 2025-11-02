package com.tuniv.backend.moderation.service;

import com.tuniv.backend.authorization.model.CommunityPermissions;
import com.tuniv.backend.authorization.model.UniversityPermissions;
import com.tuniv.backend.authorization.service.PermissionService; // ✅ ADDED
import com.tuniv.backend.moderation.dto.*;
import com.tuniv.backend.moderation.mapper.ModerationMapper;
import com.tuniv.backend.moderation.model.*;
import com.tuniv.backend.moderation.repository.ModerationLogRepository;
import com.tuniv.backend.moderation.repository.ReportRepository;
import com.tuniv.backend.shared.exception.ResourceNotFoundException;
import com.tuniv.backend.university.model.University; // ✅ ADDED
import com.tuniv.backend.university.repository.UniversityRepository; // ✅ ADDED
import com.tuniv.backend.user.model.User;
import com.tuniv.backend.user.repository.UserRepository;
import com.tuniv.backend.qa.model.Post;
import com.tuniv.backend.qa.repository.PostRepository; // Assuming this exists
import com.tuniv.backend.community.model.Community;
import com.tuniv.backend.community.repository.CommunityRepository; // Assuming this exists
import com.tuniv.backend.chat.model.Message;
import com.tuniv.backend.chat.repository.MessageRepository; // Assuming this exists

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException; // Use this for auth
import org.springframework.security.core.context.SecurityContextHolder; // ✅ ADDED
import org.springframework.security.core.userdetails.UsernameNotFoundException; // ✅ ADDED
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ModerationServiceImpl implements ModerationService {

    private final ReportRepository reportRepository;
    private final ModerationLogRepository logRepository;
    private final ModerationMapper moderationMapper;
    private final UserRepository userRepository;
    
    // Repositories to find the reported content
    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final MessageRepository messageRepository;
    private final UniversityRepository universityRepository; // ✅ ADDED

    // You will need a service to check user permissions
    private final PermissionService permissionService; // ✅ ADDED

    @Override
    public ReportSummaryDto createReport(CreateReportRequest request, User reporter) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setReason(request.reason());
        report.setDetails(request.comment());
        report.setStatus(ReportStatus.PENDING);

        // 1. Find the target entity and set polymorphic fields
        Object targetEntity = findTargetEntity(request.contentType(), request.contentId());
        report.setTarget(targetEntity);
        report.setTargetType(getTargetTypeString(request.contentType()));

        // 2. Determine the scope and denormalized IDs
        populateReportScope(report, targetEntity);
        
        Report savedReport = reportRepository.save(report);
        return moderationMapper.toReportSummaryDto(savedReport);
    }

    @Override
    public ReportDetailDto handleReport(Integer reportId, HandleReportRequest request, User moderator) {
        Report report = findReportById(reportId);
        
        // 1. Authorization Check
        checkModeratorPermissions(moderator, report.getScope(), report.getUniversityId(), report.getCommunityId());

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new IllegalStateException("Report " + reportId + " has already been handled.");
        }

        // 2. Update report status
        report.setStatus(request.newStatus());
        report.setResolvedBy(moderator);
        report.setResolutionNotes(request.moderatorNotes());
        report.setResolvedAt(Instant.now());
        
        // 3. Execute and log the moderation action
        if (request.action() != null) {
            executeModerationAction(report, request.action(), moderator);
        }

        Report savedReport = reportRepository.save(report);
        
        // 4. Return the full updated detail DTO
        return getReportDetails(savedReport.getReportId());
    }

    @Override
    public Page<ReportSummaryDto> getReportsByScope(ReportScope scope, Integer universityId, Integer communityId, Pageable pageable) {
        User currentUser = getCurrentUser(); // Helper to get from SecurityContext
        
        // 1. Authorization Check: Can the current user access this queue?
        checkModeratorPermissions(currentUser, scope, universityId, communityId);

        // 2. Fetch the correct queue from the repository
        Page<Report> reportsPage;
        ReportStatus pendingStatus = ReportStatus.PENDING; // Or combine PENDING and UNDER_REVIEW

        switch (scope) {
            case PLATFORM:
                reportsPage = reportRepository.findByScopeAndStatus(scope, pendingStatus, pageable);
                break;
            case UNIVERSITY:
                if (universityId == null) throw new IllegalArgumentException("University ID is required for UNIVERSITY scope.");
                reportsPage = reportRepository.findByScopeAndUniversityIdAndStatus(scope, universityId, pendingStatus, pageable);
                break;
            case COMMUNITY:
                if (communityId == null) throw new IllegalArgumentException("Community ID is required for COMMUNITY scope.");
                reportsPage = reportRepository.findByScopeAndCommunityIdAndStatus(scope, communityId, pendingStatus, pageable);
                break;
            case CHAT:
                 if (universityId == null) throw new IllegalArgumentException("University ID is required for CHAT scope context.");
                 // This query seems specific, ensure it's what you need
                 reportsPage = reportRepository.findByScopeAndUniversityIdAndStatusAndConversationIdIsNotNull(scope, universityId, pendingStatus, pageable);
                 break;
            default:
                throw new IllegalArgumentException("Unknown report scope: " + scope);
        }

        // 3. Map to DTOs
        return reportsPage.map(moderationMapper::toReportSummaryDto);
    }

    @Override
    public ReportDetailDto getReportDetails(Integer reportId) {
        Report report = findReportById(reportId);
        User currentUser = getCurrentUser();

        // 1. Authorization Check
        checkModeratorPermissions(currentUser, report.getScope(), report.getUniversityId(), report.getCommunityId());

        // 2. Get the target user (author of the content)
        User targetUser = getTargetUser(report.getTarget());

        // 3. Fetch moderation history for that user
        List<ModerationLogDto> historyLogs = List.of();
        if (targetUser != null) {
            historyLogs = logRepository.findTop10ByTargetUserOrderByCreatedAtDesc(targetUser)
                    .stream()
                    .map(moderationMapper::toModerationLogDto)
                    .collect(Collectors.toList());
        }
        
        // 4. Map to DTO
        return moderationMapper.toReportDetailDto(report, historyLogs);
    }

    // ========== HELPER METHODS ==========

    private void executeModerationAction(Report report, ModerationActionDto action, User moderator) {
        Object target = report.getTarget();
        User targetUser = getTargetUser(target);
        Post targetPost = (target instanceof Post) ? (Post) target : null;

        // 1. Log the action
        ModerationLog log = new ModerationLog();
        log.setModerator(moderator);
        log.setAction(action.actionType());
        log.setJustification(action.reason());
        log.setTargetUser(targetUser);
        log.setTargetPost(targetPost); // Will be null if target is not a Post
        logRepository.save(log);

        // 2. Perform the action
        switch (action.actionType()) {
            case "DELETE_POST":
                if (targetPost != null) {
                    // targetPost.softDelete("Moderator action: " + action.reason()); // Assuming a method like this exists
                    postRepository.save(targetPost);
                }
                break;
            case "BAN_USER":
                if (targetUser != null) {
                    // targetUser.setSuspended(true); // Assuming User model has this
                    // targetUser.setSuspensionExpiry(Instant.now().plusSeconds(action.banDurationInHours() * 3600));
                    userRepository.save(targetUser);
                }
                break;
            case "WARN_USER":
                // You would publish an event here to send a notification
                // eventPublisher.publishEvent(new UserWarningEvent(targetUser, action.reason()));
                break;
            // Add other cases like "REMOVE_CONTENT", "SUSPEND_USER", etc.
        }
        
        // 3. Notify user if requested
        if (action.notifyUser() && targetUser != null) {
             // eventPublisher.publishEvent(new ModerationActionTakenEvent(targetUser, ...));
        }
    }

    private void populateReportScope(Report report, Object targetEntity) {
        if (targetEntity instanceof Post) {
            Post post = (Post) targetEntity;
            if (post.getCommunity() != null) {
                report.setScope(ReportScope.COMMUNITY);
                report.setCommunityId(post.getCommunity().getCommunityId());
                if(post.getCommunity().getUniversity() != null) {
                    report.setUniversityId(post.getCommunity().getUniversity().getUniversityId());
                }
            } else if (post.getUniversityContext() != null) {
                report.setScope(ReportScope.UNIVERSITY);
                report.setUniversityId(post.getUniversityContext().getUniversityId());
            } else {
                report.setScope(ReportScope.PLATFORM); // Or default to UNIVERSITY
            }
        } else if (targetEntity instanceof Message) {
            Message message = (Message) targetEntity;
            report.setScope(ReportScope.CHAT);
            report.setConversationId(message.getConversation().getConversationId());
            if (message.getConversation().getUniversityContext() != null) {
                report.setUniversityId(message.getConversation().getUniversityContext().getUniversityId());
            }
        } else if (targetEntity instanceof Community) {
             Community community = (Community) targetEntity;
             report.setScope(ReportScope.COMMUNITY);
             report.setCommunityId(community.getCommunityId());
             if (community.getUniversity() != null) {
                 report.setUniversityId(community.getUniversity().getUniversityId());
             }
        } else if (targetEntity instanceof User) {
            report.setScope(ReportScope.PLATFORM);
        } else {
            throw new IllegalArgumentException("Unsupported entity type for reporting.");
        }
    }

    private Object findTargetEntity(ContainerType contentType, Integer contentId) {
        return switch (contentType) {
            case TOPIC, REPLY -> postRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", contentId));
            case USER_PROFILE -> userRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", contentId));
            case COMMUNITY -> communityRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Community", "id", contentId));
            case MESSAGE -> messageRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "id", contentId));
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }
    
    private String getTargetTypeString(ContainerType contentType) {
        // This maps your enum to the string values in Report.java's @AnyDiscriminatorValue
         return switch (contentType) {
            case TOPIC, REPLY -> "POST";
            case USER_PROFILE -> "USER";
            case COMMUNITY -> "COMMUNITY";
            case MESSAGE -> "MESSAGE";
            default -> throw new IllegalArgumentException("Invalid content type");
        };
    }
    
    private User getTargetUser(Object targetEntity) {
        // Gets the "owner" of the reported content
        if (targetEntity instanceof Post) return ((Post) targetEntity).getAuthor();
        if (targetEntity instanceof Message) return ((Message) targetEntity).getAuthor();
        if (targetEntity instanceof User) return (User) targetEntity;
        if (targetEntity instanceof Community) return ((Community) targetEntity).getCreator();
        return null;
    }

    private Report findReportById(Integer reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
    }
    
    // ✅ ============ FULLY IMPLEMENTED ============ ✅
    private void checkModeratorPermissions(User moderator, ReportScope scope, Integer universityId, Integer communityId) {
        boolean hasPermission = false;

        switch (scope) {
            case PLATFORM:
                // Only Platform Admins can manage platform-level reports
                hasPermission = moderator.isPlatformAdmin();
                break;

            case UNIVERSITY:
            case CHAT: // CHAT reports are scoped by universityId, so we use the university permission
                if (universityId == null) {
                    throw new AccessDeniedException("Cannot check permissions: University ID is missing for a " + scope + " report.");
                }
                // Check for platform admin (bypass) or specific university permission
                if (moderator.isPlatformAdmin()) {
                    hasPermission = true;
                } else {
                    University university = universityRepository.findById(universityId)
                            .orElseThrow(() -> new ResourceNotFoundException("University", "id", universityId));
                    hasPermission = permissionService.hasPermission(moderator.getUserId(), UniversityPermissions.UNIVERSITY_REPORT_MANAGE.getName(), university);
                }
                break;

            case COMMUNITY:
                if (communityId == null) {
                    throw new AccessDeniedException("Cannot check permissions: Community ID is missing for a COMMUNITY report.");
                }
                // Check for platform admin (bypass) or specific community permission
                if (moderator.isPlatformAdmin()) {
                    hasPermission = true;
                } else {
                    Community community = communityRepository.findById(communityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
                    hasPermission = permissionService.hasPermission(moderator.getUserId(), CommunityPermissions.COMMUNITY_REPORT_MANAGE.getName(), community);
                }
                break;
            
            default:
                 throw new IllegalArgumentException("Unknown report scope: " + scope);
        }

        if (!hasPermission) {
            throw new AccessDeniedException("You are not authorized to manage reports in this scope (" + scope + ").");
        }
    }

    // ✅ ============ FULLY IMPLEMENTED ============ ✅
    private User getCurrentUser() {
        // Get the authenticated user from Spring Security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            throw new AccessDeniedException("No authenticated user found.");
        }
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
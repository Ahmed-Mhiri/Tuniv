package com.tuniv.backend.moderation.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tuniv.backend.moderation.model.ModerationLog;
import com.tuniv.backend.user.model.User;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Integer> {

    /**
     * Finds all moderation actions taken against a specific user.
     * This is used for the 'targetUserHistory' in ReportDetailDto.
     */
    Page<ModerationLog> findByTargetUserOrderByCreatedAtDesc(User targetUser, Pageable pageable);

    /**
     * Finds all actions performed by a specific moderator.
     * Useful for moderator analytics or leaderboards.
     */
    Page<ModerationLog> findByModeratorOrderByCreatedAtDesc(User moderator, Pageable pageable);
    
    /**
     * Finds recent actions for a specific target user, used in ReportDetailDto.
     */
    List<ModerationLog> findTop10ByTargetUserOrderByCreatedAtDesc(User targetUser);
}
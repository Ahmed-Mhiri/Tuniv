package com.tuniv.backend.qa.model;

import java.time.Instant;
import com.tuniv.backend.shared.model.Auditable;
import com.tuniv.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_edit_history", indexes = {
    @Index(name = "idx_edit_history_post", columnList = "post_id, created_at DESC"),
    @Index(name = "idx_edit_history_author", columnList = "edited_by_user_id"),
    @Index(name = "idx_edit_history_version", columnList = "post_id, version_number")
})
@Getter
@Setter
@NoArgsConstructor
public class PostEditHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== CORE RELATIONSHIPS ==========
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_user_id")
    private User editedBy;

    // ========== EDIT CONTENT ==========
    @Column(name = "previous_body", columnDefinition = "TEXT")
    private String previousBody;

    @Column(name = "edit_reason", length = 500)
    private String editReason;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt = Instant.now();

    // ========== METADATA ==========
    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Column(name = "word_count_change")
    private Integer wordCountChange;

    // ========== CONSTRUCTORS ==========
    public PostEditHistory(Post post, String previousBody, User editedBy, String editReason) {
        this.post = post;
        this.previousBody = previousBody;
        this.editedBy = editedBy;
        this.editReason = editReason;
        this.editedAt = Instant.now();
    }
}
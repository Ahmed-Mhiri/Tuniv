package com.tuniv.backend.user.model;

import com.tuniv.backend.shared.model.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reputation_logs", indexes = {
    @Index(name = "idx_reputation_log_user", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_reputation_log_source", columnList = "source_entity_type, source_entity_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ReputationLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ReputationActionType actionType;

    @NotNull
    @Column(name = "point_change", nullable = false)
    private Integer pointChange;

    // Optional: To link back to the source of the change
    @Column(name = "source_entity_type")
    private String sourceEntityType; // e.g., "REPLY", "VOTE"

    @Column(name = "source_entity_id")
    private Integer sourceEntityId;

    // New field to track the user's total reputation after this change
    @Column(name = "resulting_reputation")
    private Integer resultingReputation;

    public ReputationLog(User user, ReputationActionType actionType, Integer pointChange) {
    this.user = user;
    this.actionType = actionType;
    this.pointChange = pointChange;
}

    public ReputationLog(User user, ReputationActionType actionType, Integer pointChange, 
                        String sourceEntityType, Integer sourceEntityId) {
        this(user, actionType, pointChange);
        this.sourceEntityType = sourceEntityType;
        this.sourceEntityId = sourceEntityId;
    }


}

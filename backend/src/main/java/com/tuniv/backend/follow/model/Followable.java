package com.tuniv.backend.follow.model;

import com.tuniv.backend.shared.model.Auditable;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "followables")
@Getter
@Setter
@NoArgsConstructor
public class Followable extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ✅ CHANGED to Long for scalability

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private FollowableType entityType;

    // ✅ Optional: Denormalized counter for performance
    @Column(name = "follower_count", nullable = false)
    private Integer followerCount = 0;

    public Followable(FollowableType entityType) {
        this.entityType = entityType;
    }
}
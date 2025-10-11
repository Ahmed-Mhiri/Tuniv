package com.tuniv.backend.chat.model;

import java.util.HashSet;
import java.util.Set;

import com.tuniv.backend.authorization.model.Permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "conversation_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ConversationRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    // ✅ Link to conversation (NULL for system roles)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    // ✅ Permissions for chat actions
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "conversation_role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 0;
}
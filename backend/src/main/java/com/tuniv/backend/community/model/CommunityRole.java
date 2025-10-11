package com.tuniv.backend.community.model;

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
@Table(name = "community_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "community_id"}) // ✅ Prevents duplicate role names within the same community
})
@Getter
@Setter
@NoArgsConstructor
public class CommunityRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    // ✅ ADDED: Link to a community. Will be NULL for system roles.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    // ✅ FIXED: Changed from EAGER to LAZY to prevent performance issues
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "community_role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 0;
}
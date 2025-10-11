package com.tuniv.backend.authorization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    private Integer id;

    @NotBlank
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_scope", nullable = false)
    private PermissionScope scope = PermissionScope.UNIVERSITY;

    public enum PermissionScope {
    UNIVERSITY,
    COMMUNITY,
    PLATFORM,
    CHAT  // ✅ ADDED: New scope for chat permissions
    }
}
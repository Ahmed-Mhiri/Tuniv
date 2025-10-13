package com.tuniv.backend.authorization.repository;

import com.tuniv.backend.authorization.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);
    
    Set<Permission> findByNameIn(Set<String> names);
    
    @Query("SELECT p FROM Permission p WHERE p.name LIKE :pattern")
    Set<Permission> findByPattern(@Param("pattern") String pattern);
    
    boolean existsByName(String name);
    
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    Set<Permission> findByRoleId(@Param("roleId") Long roleId);
}
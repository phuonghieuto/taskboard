package com.phuonghieuto.backend.task_service.model.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Base entity class named {@link BaseEntity} with common fields for audit tracking and lifecycle management.
 * Provides automatic population of audit fields using JPA lifecycle annotations.
 */
@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    /**
     * Sets the createdBy and createdAt fields before persisting the entity.
     * If no authenticated user is found, sets createdBy to "anonymousUser".
     */
    @PrePersist
    public void prePersist() {
        this.createdBy = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .map(principal -> {
                    if (principal instanceof UserDetails) {
                        return ((UserDetails) principal).getUsername();
                    } else {
                        return principal.toString();
                    }
                })
                .orElse("anonymousUser");
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Sets the updatedBy and updatedAt fields before updating the entity.
     * If no authenticated user is found, sets updatedBy to "anonymousUser".
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedBy = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .map(principal -> {
                    if (principal instanceof UserDetails) {
                        return ((UserDetails) principal).getUsername();
                    } else {
                        return principal.toString();
                    }
                })
                .orElse("anonymousUser");
        this.updatedAt = LocalDateTime.now();
    }

}
package com.phuonghieuto.backend.task_service.model.collaboration.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "board_invitations")
public class BoardInvitationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;
    
    @Column(nullable = false)
    private String inviterUserId;
    
    @Column(nullable = false)
    private String inviteeEmail;
    
    @Column
    private String inviteeUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;
    
    @Column(nullable = false, unique = true)
    private String token;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = InvitationStatus.PENDING;
        }
    }
}
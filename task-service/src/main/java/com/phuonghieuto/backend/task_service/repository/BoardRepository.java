package com.phuonghieuto.backend.task_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, String> {
    List<BoardEntity> findByOwnerId(String ownerId);
    List<BoardEntity> findByOwnerIdOrCollaboratorIdsContains(String ownerId, String collaboratorId);
}
package com.phuonghieuto.backend.task_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, String> {
    List<TableEntity> findByBoardIdOrderByOrderIndexAsc(String boardId);
}
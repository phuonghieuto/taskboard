package com.phuonghieuto.backend.task_service.model.task.entity;

import java.util.Set;

import com.phuonghieuto.backend.task_service.model.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = {"board", "tasks"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tables")
public class TableEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "ORDER_INDEX")
    private int orderIndex;

    @ManyToOne
    @JoinColumn(name = "BOARD_ID")
    private BoardEntity board;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskEntity> tasks;
}

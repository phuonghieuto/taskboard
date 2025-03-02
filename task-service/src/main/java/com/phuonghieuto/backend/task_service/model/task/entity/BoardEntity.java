package com.phuonghieuto.backend.task_service.model.task.entity;

import java.util.Set;

import com.phuonghieuto.backend.task_service.model.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boards")
public class BoardEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "OWNER_ID")
    private String ownerId;

    @ElementCollection
    @CollectionTable(name = "board_users", joinColumns = @JoinColumn(name = "BOARD_ID"))
    @Column(name = "USER_ID")
    private Set<String> collaboratorIds;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TableEntity> tables;
}

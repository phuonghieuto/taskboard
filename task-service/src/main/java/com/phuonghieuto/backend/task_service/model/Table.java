package com.phuonghieuto.backend.task_service.model;

import java.util.Set;

import com.phuonghieuto.backend.task_service.model.common.BaseDomainModel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Table extends BaseDomainModel {
    private String id;
    private String name;
    private int orderIndex;
    private Board board;
    private Set<Task> tasks;

}

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
public class Board extends BaseDomainModel {
    private String id;
    private String name;
    private String ownerId;
    private Set<String> collaboratorIds;
    private Set<Table> tables;
    
}

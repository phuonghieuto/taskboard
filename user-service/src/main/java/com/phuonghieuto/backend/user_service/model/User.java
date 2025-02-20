package com.phuonghieuto.backend.user_service.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.phuonghieuto.backend.user_service.model.common.BaseDomainModel;
import com.phuonghieuto.backend.user_service.model.user.enums.UserStatus;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseDomainModel {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserStatus userStatus;
    private String password;
}
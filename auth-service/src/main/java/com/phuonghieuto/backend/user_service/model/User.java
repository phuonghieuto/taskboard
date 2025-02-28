package com.phuonghieuto.backend.user_service.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import com.phuonghieuto.backend.user_service.model.common.BaseDomainModel;
import com.phuonghieuto.backend.user_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.user_service.model.user.enums.UserType;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class User extends BaseDomainModel {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserType userType;
    private UserStatus userStatus;
    private String password;
}
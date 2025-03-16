package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;


@Mapper(componentModel = "spring")
public interface UserEntityToUserMapper extends BaseMapper<UserEntity, User> {

    @Override
    User map(UserEntity source);

}

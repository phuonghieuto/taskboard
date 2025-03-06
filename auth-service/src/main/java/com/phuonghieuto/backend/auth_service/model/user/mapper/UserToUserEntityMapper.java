package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;


@Mapper
public interface UserToUserEntityMapper extends BaseMapper<User, UserEntity> {

    
    @Override
    UserEntity map(User source);

    static UserToUserEntityMapper initialize() {
        return Mappers.getMapper(UserToUserEntityMapper.class);
    }

}

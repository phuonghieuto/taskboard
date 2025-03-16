package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;


@Mapper(componentModel = "spring")
public interface UserToUserEntityMapper extends BaseMapper<User, UserEntity> {

    
    @Override
    UserEntity map(User source);

}

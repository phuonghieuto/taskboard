package com.phuonghieuto.backend.user_service.mapper;

import com.phuonghieuto.backend.user_service.dto.UserCreateDTO;
import com.phuonghieuto.backend.user_service.dto.UserUpdateDTO;
import com.phuonghieuto.backend.user_service.dto.UserDTO;
import com.phuonghieuto.backend.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    User toUser(UserCreateDTO userCreateDTO);
    UserDTO toUserDTO(User user);
    void updateUserFromDTO(UserUpdateDTO userUpdateDTO, @MappingTarget User user);
}
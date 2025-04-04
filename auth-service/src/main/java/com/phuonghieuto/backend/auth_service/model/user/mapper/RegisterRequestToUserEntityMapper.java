package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface RegisterRequestToUserEntityMapper extends BaseMapper<RegisterRequestDTO, UserEntity> {
    
    @Named("mapForSaving")
    default UserEntity mapForSaving(RegisterRequestDTO userRegisterRequest) {

        return UserEntity.builder()
                .email(userRegisterRequest.getEmail())
                .firstName(userRegisterRequest.getFirstName())
                .lastName(userRegisterRequest.getLastName())
                .username(userRegisterRequest.getUsername())
                .phoneNumber(userRegisterRequest.getPhoneNumber())
                .build();
    }
    
}
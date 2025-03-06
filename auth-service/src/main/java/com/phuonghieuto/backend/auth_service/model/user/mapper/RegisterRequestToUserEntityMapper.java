package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

@Mapper
public interface RegisterRequestToUserEntityMapper extends BaseMapper<RegisterRequestDTO, UserEntity> {
    
    @Named("mapForSaving")
    default UserEntity mapForSaving(RegisterRequestDTO userRegisterRequest) {

        return UserEntity.builder()
                .email(userRegisterRequest.getEmail())
                .firstName(userRegisterRequest.getFirstName())
                .lastName(userRegisterRequest.getLastName())
                .phoneNumber(userRegisterRequest.getPhoneNumber())
                .build();
    }

    
    static RegisterRequestToUserEntityMapper initialize() {
        return Mappers.getMapper(RegisterRequestToUserEntityMapper.class);
    }

}
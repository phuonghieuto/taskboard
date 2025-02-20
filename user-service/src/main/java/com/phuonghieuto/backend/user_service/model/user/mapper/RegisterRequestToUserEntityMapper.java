package com.phuonghieuto.backend.user_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.user_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.user_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.user_service.model.user.entity.UserEntity;

/**
 * Mapper interface for converting between {@link RegisterRequest} and {@link UserEntity}.
 * This mapper handles the transformation of user registration request data into a user entity
 * for persistence in the database.
 */
@Mapper
public interface RegisterRequestToUserEntityMapper extends BaseMapper<RegisterRequestDTO, UserEntity> {
    /**
     * Maps a {@link RegisterRequest} to a {@link UserEntity} for saving.
     * This method maps the user's registration request to a {@link UserEntity} with appropriate
     * user type based on the role specified in the request.
     *
     * @param userRegisterRequest the registration request containing user details
     * @return a {@link UserEntity} with mapped values
     */
    @Named("mapForSaving")
    default UserEntity mapForSaving(RegisterRequestDTO userRegisterRequest) {

        return UserEntity.builder()
                .email(userRegisterRequest.getEmail())
                .firstName(userRegisterRequest.getFirstName())
                .lastName(userRegisterRequest.getLastName())
                .phoneNumber(userRegisterRequest.getPhoneNumber())
                .build();
    }

    /**
     * Initializes the {@link RegisterRequestToUserEntityMapper} mapper.
     *
     * @return a new instance of {@link RegisterRequestToUserEntityMapper}
     */
    static RegisterRequestToUserEntityMapper initialize() {
        return Mappers.getMapper(RegisterRequestToUserEntityMapper.class);
    }

}
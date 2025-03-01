package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;


/**
 * Mapper interface for converting between {@link UserEntity} and {@link User}.
 * This mapper handles the transformation of user entity data into a user domain model.
 */
@Mapper
public interface UserEntityToUserMapper extends BaseMapper<UserEntity, User> {

    /**
     * Maps a {@link UserEntity} to a {@link User}.
     * This method performs the mapping of the user entity to the corresponding user domain model.
     *
     * @param source the user entity to be mapped
     * @return a {@link User} with mapped values
     */
    @Override
    User map(UserEntity source);

    /**
     * Initializes the {@link UserEntityToUserMapper} mapper.
     *
     * @return a new instance of {@link UserEntityToUserMapper}
     */
    static UserEntityToUserMapper initialize() {
        return Mappers.getMapper(UserEntityToUserMapper.class);
    }

}

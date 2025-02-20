package com.phuonghieuto.backend.user_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.user_service.model.User;
import com.phuonghieuto.backend.user_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.user_service.model.user.entity.UserEntity;

/**
 * Mapper interface for converting between {@link User} and {@link UserEntity}.
 * This mapper handles the transformation of user domain model data into a user entity
 * for persistence in the database.
 */
@Mapper
public interface UserToUserEntityMapper extends BaseMapper<User, UserEntity> {

    /**
     * Maps a {@link User} to a {@link UserEntity}.
     * This method performs the mapping of the user domain model to the corresponding user entity.
     *
     * @param source the user to be mapped
     * @return a {@link UserEntity} with mapped values
     */
    @Override
    UserEntity map(User source);

    /**
     * Initializes the {@link UserToUserEntityMapper} mapper.
     *
     * @return a new instance of {@link UserToUserEntityMapper}
     */
    static UserToUserEntityMapper initialize() {
        return Mappers.getMapper(UserToUserEntityMapper.class);
    }

}

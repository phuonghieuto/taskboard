package com.phuonghieuto.backend.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    boolean existsUserEntityByEmail(final String email);
    Optional<UserEntity> findUserEntityByEmail(final String email);
}
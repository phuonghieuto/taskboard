package com.phuonghieuto.backend.auth_service.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.phuonghieuto.backend.auth_service.model.user.entity.InvalidTokenEntity;

@Repository
public interface InvalidTokenRepository extends JpaRepository<InvalidTokenEntity, String> {
    Optional<InvalidTokenEntity> findByTokenId(final String tokenId);
}
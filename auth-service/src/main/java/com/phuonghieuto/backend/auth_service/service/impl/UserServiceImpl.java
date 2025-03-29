package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.exception.UserAlreadyExistException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.auth_service.model.user.mapper.RegisterRequestToUserEntityMapper;
import com.phuonghieuto.backend.auth_service.model.user.mapper.UserEntityToUserMapper;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegisterRequestToUserEntityMapper registerRequestToUserEntityMapper;
    private final UserEntityToUserMapper userEntityToUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationProducer notificationProducer;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", allEntries = true),
        @CacheEvict(value = "userByEmail", allEntries = true)
    })
    public User registerUser(RegisterRequestDTO registerRequest) {
        log.info("Registering new user with email: {}", registerRequest.getEmail());

        if (userRepository.existsUserEntityByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistException(
                    "The email is already used for another account: " + registerRequest.getEmail());
        }

        if (userRepository.existsUserEntityByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistException(
                    "The username is already used for another account: " + registerRequest.getUsername());
        }

        final UserEntity userEntityToBeSave = registerRequestToUserEntityMapper.mapForSaving(registerRequest);
        userEntityToBeSave.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userEntityToBeSave.setUserStatus(UserStatus.PASSIVE); // Set as PASSIVE until confirmed
        userEntityToBeSave.setEmailConfirmed(false);

        UserEntity savedUserEntity = userRepository.save(userEntityToBeSave);

        notificationProducer.sendEmailConfirmationMessage(savedUserEntity);

        return userEntityToUserMapper.map(savedUserEntity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userById", key = "#userId")
    public UserEmailDTO getUserEmail(String userId) {
        log.info("Looking up user email for userId: {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        log.debug("Found user email for userId {}: {}", userId, userEntity.getEmail());
        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userByEmail", key = "#email")
    public UserEmailDTO getUserIdFromEmail(String email) {
        log.info("Looking up userId for email: {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        log.debug("Found userId for email {}: {}", email, userEntity.getId());
        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#result.userId"),
        @CacheEvict(value = "userByEmail", key = "#result.email")
    })
    public boolean confirmEmail(String token) {
        log.info("Confirming email with token: {}", token);

        UserEntity user = userRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (LocalDateTime.now().isAfter(user.getConfirmationTokenExpiry())) {
            throw new RuntimeException("Confirmation token expired");
        }

        user.setEmailConfirmed(true);
        user.setUserStatus(UserStatus.ACTIVE); // Activate the user
        user.setConfirmationToken(null);
        user.setConfirmationTokenExpiry(null);
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Email confirmed successfully for user: {}", savedUser.getId());
        return true;
    }
}
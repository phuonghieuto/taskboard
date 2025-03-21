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
    public User registerUser(RegisterRequestDTO registerRequest) {

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
    @Transactional
    public UserEmailDTO getUserEmail(String userId) {
        log.info("UserServiceImpl | getUserEmail | userId: {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

    @Override
    @Transactional
    public UserEmailDTO getUserIdFromEmail(String email) {
        log.info("UserServiceImpl | getUserIdFromEmail | email: {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

    @Override
    @Transactional
    public boolean confirmEmail(String token) {
        UserEntity user = userRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (LocalDateTime.now().isAfter(user.getConfirmationTokenExpiry())) {
            throw new RuntimeException("Confirmation token expired");
        }

        user.setEmailConfirmed(true);
        user.setUserStatus(UserStatus.ACTIVE); // Activate the user
        user.setConfirmationToken(null);
        user.setConfirmationTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

}
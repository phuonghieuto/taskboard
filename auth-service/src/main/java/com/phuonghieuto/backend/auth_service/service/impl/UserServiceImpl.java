package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.exception.UserAlreadyExistException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.mapper.RegisterRequestToUserEntityMapper;
import com.phuonghieuto.backend.auth_service.model.user.mapper.UserEntityToUserMapper;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RegisterRequestToUserEntityMapper registerRequestToUserEntityMapper = RegisterRequestToUserEntityMapper
            .initialize();
    private final UserEntityToUserMapper userEntityToUserMapper = UserEntityToUserMapper.initialize();
    private final PasswordEncoder passwordEncoder;

    @Override
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

        UserEntity savedUserEntity = userRepository.save(userEntityToBeSave);

        return userEntityToUserMapper.map(savedUserEntity);
    }

    @Override
    public UserEmailDTO getUserEmail(String userId) {
        log.info("UserServiceImpl | getUserEmail | userId: {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

    @Override
    public UserEmailDTO getUserIdFromEmail(String email) {
        log.info("UserServiceImpl | getUserIdFromEmail | email: {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return UserEmailDTO.builder().userId(userEntity.getId()).email(userEntity.getEmail()).build();
    }

}
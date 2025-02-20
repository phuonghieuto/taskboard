package com.phuonghieuto.backend.user_service.service.impl;

import com.phuonghieuto.backend.user_service.exception.UserAlreadyExistsException;
import com.phuonghieuto.backend.user_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.user_service.model.User;
import com.phuonghieuto.backend.user_service.repository.UserRepository;
import com.phuonghieuto.backend.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    

    @Override
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id).map(userMapper::toUserDTO);
    }

    @Override
    public UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        userMapper.updateUserFromDTO(userUpdateDTO, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toUserDTO(updatedUser);
    }
}
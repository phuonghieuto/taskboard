package com.phuonghieuto.backend.user_service.service;

import com.phuonghieuto.backend.user_service.dto.UserCreateDTO;
import com.phuonghieuto.backend.user_service.dto.UserUpdateDTO;
import com.phuonghieuto.backend.user_service.dto.UserDTO;
import com.phuonghieuto.backend.user_service.entity.User;
import com.phuonghieuto.backend.user_service.exception.UserAlreadyExistsException;
import com.phuonghieuto.backend.user_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.user_service.mapper.UserMapper;
import com.phuonghieuto.backend.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper = UserMapper.INSTANCE;

    public UserDTO createUser(UserCreateDTO userCreateDTO) {
        if (userRepository.findByUsername(userCreateDTO.getUsername()) != null) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.findByEmail(userCreateDTO.getEmail()) != null) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        User user = userMapper.toUser(userCreateDTO);
        User savedUser = userRepository.save(user);
        return userMapper.toUserDTO(savedUser);
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id).map(userMapper::toUserDTO);
    }

    public UserDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
        userMapper.updateUserFromDTO(userUpdateDTO, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toUserDTO(updatedUser);
    }
}
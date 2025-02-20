package com.phuonghieuto.backend.user_service.service;



import com.phuonghieuto.backend.user_service.model.Token;
import com.phuonghieuto.backend.user_service.model.User;
import com.phuonghieuto.backend.user_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.user_service.model.user.dto.request.RegisterRequestDTO;

public interface UserService {
    User registerUser(final RegisterRequestDTO registerRequest);
    Token login(final LoginRequestDTO loginRequest);
    
}
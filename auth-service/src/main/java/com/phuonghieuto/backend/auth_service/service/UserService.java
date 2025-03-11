package com.phuonghieuto.backend.auth_service.service;



import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;

public interface UserService {
    User registerUser(final RegisterRequestDTO registerRequest);
    UserEmailDTO getUserEmail(String userId);
    UserEmailDTO getUserIdFromEmail(String email);
}
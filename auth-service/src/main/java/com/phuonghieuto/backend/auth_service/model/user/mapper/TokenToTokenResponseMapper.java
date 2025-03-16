package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;



@Mapper(componentModel = "spring")
public interface TokenToTokenResponseMapper extends BaseMapper<Token, TokenResponseDTO> {

    @Override
    TokenResponseDTO map(Token source);

}

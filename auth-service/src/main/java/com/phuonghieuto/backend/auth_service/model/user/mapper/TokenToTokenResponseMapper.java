package com.phuonghieuto.backend.auth_service.model.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.common.mapper.BaseMapper;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;


/**
 * Mapper interface for converting between {@link Token} and {@link TokenResponseDTO}.
 * This mapper handles the transformation of token data into a response format.
 */
@Mapper
public interface TokenToTokenResponseMapper extends BaseMapper<Token, TokenResponseDTO> {

    /**
     * Maps a {@link Token} to a {@link TokenResponseDTO}.
     * This method performs the mapping of the token object to the corresponding token response.
     * @param source the token to be mapped
     * @return a {@link TokenResponseDTO} with mapped values
     */
    @Override
    TokenResponseDTO map(Token source);

    /**
     * Initializes the {@link TokenToTokenResponseMapper} mapper.
     *
     * @return a new instance of {@link TokenToTokenResponseMapper}
     */
    static TokenToTokenResponseMapper initialize() {
        return Mappers.getMapper(TokenToTokenResponseMapper.class);
    }

}

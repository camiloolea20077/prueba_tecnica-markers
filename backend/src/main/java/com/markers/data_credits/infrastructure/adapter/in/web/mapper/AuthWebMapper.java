package com.markers.data_credits.infrastructure.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.markers.data_credits.domain.model.AuthSession;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.AuthResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.UserResponse;

/**
 * Conversión dominio → DTOs web de autenticación.
 */
@Mapper
public interface AuthWebMapper {

    @Mapping(target = "role", source = "role.code")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "permissions", source = "role.permissions")
    UserResponse toResponse(User user);

    @Mapping(target = "token", source = "token.value")
    @Mapping(target = "expiresAt", source = "token.expiresAt")
    @Mapping(target = "tokenType", constant = "Bearer")
    AuthResponse toResponse(AuthSession session);
}

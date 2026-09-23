package com.markers.data_credits.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

/**
 * Conversión entidad JPA → modelo de dominio de usuarios.
 */
@Mapper
public interface UserPersistenceMapper {

    @Mapping(target = "passwordHash", source = "password")
    User toDomain(UserEntity entity);

    Role toDomain(RoleEntity entity);

    default String toCode(PermissionEntity permission) {
        return permission.getCode();
    }
}

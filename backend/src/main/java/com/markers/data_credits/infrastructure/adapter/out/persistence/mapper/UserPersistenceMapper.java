package com.markers.data_credits.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.UserEntity;

/**
 * Conversión entidad JPA → modelo de dominio de usuarios y roles.
 */
@Mapper
public interface UserPersistenceMapper {

    Role toDomain(RoleEntity entity);

    default String toCode(PermissionEntity permission) {
        return permission.getCode();
    }

    /** Manual: MapStruct confunde {@code User.withActive(boolean)} con un setter fluido. */
    default User toDomain(UserEntity e) {
        if (e == null) {
            return null;
        }
        return new User(e.getId(), e.getFullName(), e.getEmail(), e.getPassword(), e.isActive(),
                toDomain(e.getRole()), e.getCreatedAt());
    }
}

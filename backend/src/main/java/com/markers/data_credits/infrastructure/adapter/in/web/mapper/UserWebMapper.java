package com.markers.data_credits.infrastructure.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.AdminUserResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.PageResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.RoleResponse;

/**
 * Conversión dominio → DTOs web de administración de usuarios.
 */
@Mapper
public interface UserWebMapper {

    @Mapping(target = "role", source = "role.code")
    @Mapping(target = "roleName", source = "role.name")
    AdminUserResponse toResponse(User user);

    List<AdminUserResponse> toResponses(List<User> users);

    RoleResponse toResponse(Role role);

    List<RoleResponse> toRoleResponses(List<Role> roles);

    default PageResponse<AdminUserResponse> toPageResponse(PageResult<User> page) {
        return new PageResponse<>(toResponses(page.content()), page.page(), page.size(), page.totalElements(),
                page.totalPages());
    }
}

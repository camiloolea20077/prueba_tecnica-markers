package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.model.Permissions;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.CreateUserCommand;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.UserSearch;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.ChangeUserStatusRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.CreateUserRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.UpdateUserRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.AdminUserResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.PageResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.RoleResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.UserWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;
import com.markers.data_credits.infrastructure.security.AuthenticatedUser;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * CRUD de usuarios y consulta de roles (rol ADMIN + permiso USER_MANAGE).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private static final String CAN_MANAGE = "hasAuthority('" + Permissions.USER_MANAGE + "')";

    private final ManageUsersUseCase useCase;
    private final UserWebMapper mapper;
    private final BlockingExecutor executor;

    public AdminUserController(ManageUsersUseCase useCase, UserWebMapper mapper, BlockingExecutor executor) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    /** {@code GET /api/admin/users?q=&role=ADMIN&active=true&page=0&size=10} */
    @GetMapping("/users")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserSearch search = new UserSearch(q, role, active, page, size);
        return executor.run(() -> useCase.search(search))
                .map(mapper::toPageResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Usuarios", body)));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<AdminUserResponse>>> findById(@PathVariable Long id) {
        return executor.run(() -> useCase.findById(id))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Usuario", body)));
    }

    /** @return 201; 400 formato; 409 correo repetido; 422 rol inexistente */
    @PostMapping("/users")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<AdminUserResponse>>> create(@Valid @RequestBody CreateUserRequest r) {
        CreateUserCommand command = new CreateUserCommand(r.fullName(), r.email(), r.password(), r.role(), r.active());
        return executor.run(() -> useCase.create(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.of(HttpStatus.CREATED, "Usuario creado", body)));
    }

    /** @return 200; 409 correo repetido; 422 autoprotección o último administrador */
    @PutMapping("/users/{id}")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<AdminUserResponse>>> update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest r) {
        UpdateUserCommand command = new UpdateUserCommand(r.fullName(), r.email(), r.role(), r.active(), r.password());
        return executor.run(() -> useCase.update(id, command, principal.id()))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Usuario actualizado", body)));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<AdminUserResponse>>> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserStatusRequest r) {
        return executor.run(() -> useCase.changeStatus(id, r.active(), principal.id()))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok(
                        body.active() ? "Usuario activado" : "Usuario desactivado", body)));
    }

    /** @return 200; 409 tiene créditos; 422 autoprotección o último administrador */
    @DeleteMapping("/users/{id}")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<Object>>> delete(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        return executor.run(() -> {
                    useCase.delete(id, principal.id());
                    return ApiResponse.ok("Usuario eliminado", null);
                })
                .map(ResponseEntity::ok);
    }

    /** {@code GET /api/admin/roles} → roles con permisos. */
    @GetMapping("/roles")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<List<RoleResponse>>>> roles() {
        return executor.run(useCase::findRoles)
                .map(mapper::toRoleResponses)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Roles", body)));
    }
}

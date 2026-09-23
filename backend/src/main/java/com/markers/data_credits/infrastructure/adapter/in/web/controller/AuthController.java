package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.port.in.AuthenticateUseCase;
import com.markers.data_credits.domain.port.in.AuthenticateUseCase.LoginCommand;
import com.markers.data_credits.domain.port.in.GetCurrentUserUseCase;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.LoginRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.AuthResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.UserResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.AuthWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;
import com.markers.data_credits.infrastructure.security.AuthenticatedUser;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * Autenticación: inicio de sesión y usuario en sesión.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final AuthWebMapper mapper;
    private final BlockingExecutor executor;

    public AuthController(AuthenticateUseCase authenticateUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          AuthWebMapper mapper,
                          BlockingExecutor executor) {
        this.authenticateUseCase = authenticateUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    /**
     * Inicia sesión con correo y contraseña.
     * <p>
     * Endpoint público: {@code POST /api/auth/login}
     * </p>
     *
     * @return 200 con el token; 400 si faltan datos; 401 si las credenciales son incorrectas;
     *         403 si el usuario está inactivo
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());
        return executor.run(() -> authenticateUseCase.login(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Inicio de sesión exitoso", body)));
    }

    /**
     * Devuelve el usuario autenticado con su rol y permisos vigentes.
     * <p>
     * Endpoint: {@code GET /api/auth/me} (requiere token)
     * </p>
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return executor.run(() -> getCurrentUserUseCase.getCurrentUser(principal.email()))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Usuario autenticado", body)));
    }
}

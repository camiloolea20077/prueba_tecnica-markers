package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.exception.InactiveUserException;
import com.markers.data_credits.domain.exception.InvalidCredentialsException;
import com.markers.data_credits.domain.model.AuthSession;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.AuthenticateUseCase;
import com.markers.data_credits.domain.port.in.GetCurrentUserUseCase;
import com.markers.data_credits.infrastructure.security.JwtTokenProvider;
import com.markers.data_credits.support.TestUsers;
import com.markers.data_credits.support.WebLayerTest;

import reactor.core.publisher.Mono;

/**
 * Login, manejo de errores HTTP y reglas de autorización por rol y por permiso.
 */
@WebFluxTest(controllers = {AuthController.class, AuthControllerTest.ProbeController.class})
@WebLayerTest
@Import(AuthControllerTest.ProbeController.class)
class AuthControllerTest {

    /** Endpoints de prueba para verificar las dos barreras de autorización. */
    @RestController
    static class ProbeController {

        @GetMapping("/api/admin/probe")
        Map<String, String> adminOnly() {
            return Map.of("ok", "admin");
        }

        /** Con seguridad reactiva, un método con @PreAuthorize debe devolver Mono/Flux. */
        @PatchMapping("/api/probe/approve")
        @PreAuthorize("hasAuthority('CREDIT_APPROVE')")
        Mono<Map<String, String>> needsApprovePermission() {
            return Mono.just(Map.of("ok", "approve"));
        }
    }

    @Autowired
    private WebTestClient client;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AuthenticateUseCase authenticateUseCase;
    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generate(user).value();
    }

    private WebTestClient.ResponseSpec login(String body) {
        return client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    // ---------- Login ----------

    @Test
    @DisplayName("POST /login correcto → 200 con token, rol y permisos")
    void loginOk() {
        User admin = TestUsers.admin();
        when(authenticateUseCase.login(any()))
                .thenReturn(new AuthSession(tokenProvider.generate(admin), admin));

        login("{\"email\":\"admin@test.com\",\"password\":\"123\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.error").isEqualTo(false)
                .jsonPath("$.data.tokenType").isEqualTo("Bearer")
                .jsonPath("$.data.token").isNotEmpty()
                .jsonPath("$.data.user.role").isEqualTo("ADMIN")
                .jsonPath("$.data.user.password").doesNotExist()
                .jsonPath("$.data.user.permissions[?(@ == 'CREDIT_APPROVE')]").exists();
    }

    @Test
    @DisplayName("POST /login con datos inválidos → 400 con error por campo")
    void loginValidation() {
        login("{\"email\":\"no-es-correo\",\"password\":\"\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo(true)
                .jsonPath("$.data.email").isEqualTo("El correo no tiene un formato válido")
                .jsonPath("$.data.password").isEqualTo("La contraseña es obligatoria");
    }

    @Test
    @DisplayName("POST /login con JSON mal formado → 400")
    void loginMalformedJson() {
        login("{mal").expectStatus().isBadRequest().expectBody().jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("POST /login con credenciales incorrectas → 401")
    void loginBadCredentials() {
        when(authenticateUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        login("{\"email\":\"admin@test.com\",\"password\":\"mala\"}")
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("POST /login con usuario inactivo → 403")
    void loginInactive() {
        when(authenticateUseCase.login(any())).thenThrow(new InactiveUserException());

        login("{\"email\":\"inactivo@test.com\",\"password\":\"123\"}").expectStatus().isForbidden();
    }

    // ---------- /me ----------

    @Test
    @DisplayName("GET /me sin token → 401 con formato ApiResponse")
    void meWithoutToken() {
        client.get().uri("/api/auth/me").exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo(true);
    }

    @Test
    @DisplayName("GET /me con token válido → 200 con el usuario")
    void meWithToken() {
        User user = TestUsers.user();
        when(getCurrentUserUseCase.getCurrentUser("usuario@test.com")).thenReturn(user);

        client.get().uri("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.email").isEqualTo("usuario@test.com")
                .jsonPath("$.data.role").isEqualTo("USER");
    }

    @Test
    @DisplayName("GET /me con token expirado/alterado → 401")
    void meWithInvalidToken() {
        client.get().uri("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi").exchange()
                .expectStatus().isUnauthorized();
    }

    // ---------- Autorización ----------

    @Test
    @DisplayName("/api/admin/** con rol USER → 403")
    void adminRouteForbiddenForUser() {
        client.get().uri("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.message").isEqualTo("No tiene permisos para realizar esta acción");
    }

    @Test
    @DisplayName("/api/admin/** con rol ADMIN → 200")
    void adminRouteAllowedForAdmin() {
        client.get().uri("/api/admin/probe").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("@PreAuthorize CREDIT_APPROVE: USER → 403, ADMIN → 200")
    void permissionBasedAuthorization() {
        client.patch().uri("/api/probe/approve").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isForbidden();

        client.patch().uri("/api/probe/approve").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("token válido con un carácter extra al final → 401")
    void tokenWithTrailingCharacterIsRejected() {
        String token = tokenProvider.generate(TestUsers.user()).value();

        client.get().uri("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x").exchange()
                .expectStatus().isUnauthorized();
    }
}

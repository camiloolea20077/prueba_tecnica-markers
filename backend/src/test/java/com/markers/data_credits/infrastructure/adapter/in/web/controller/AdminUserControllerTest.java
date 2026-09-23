package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.markers.data_credits.domain.exception.EmailAlreadyExistsException;
import com.markers.data_credits.domain.exception.UserInUseException;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.markers.data_credits.infrastructure.security.JwtTokenProvider;
import com.markers.data_credits.support.TestUsers;
import com.markers.data_credits.support.WebLayerTest;

/**
 * CRUD de usuarios: permisos, validación, códigos HTTP y que nunca se exponga la contraseña.
 */
@WebFluxTest(controllers = AdminUserController.class)
@WebLayerTest
class AdminUserControllerTest {

    private static final String VALID_USER = """
            {"fullName":"Ana Pérez","email":"ana@test.com","password":"secreta1","role":"USER"}""";

    @Autowired
    private WebTestClient client;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private ManageUsersUseCase useCase;

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generate(user).value();
    }

    private WebTestClient.ResponseSpec send(String method, String uri, User as, String body) {
        return client.method(org.springframework.http.HttpMethod.valueOf(method)).uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    @Test
    @DisplayName("USER no accede a la administración de usuarios")
    void userForbidden() {
        client.get().uri("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user()))
                .exchange().expectStatus().isForbidden();
        send("POST", "/api/admin/users", TestUsers.user(), VALID_USER).expectStatus().isForbidden();
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("listar → página sin contraseñas")
    void search() {
        when(useCase.search(any())).thenReturn(new PageResult<>(List.of(TestUsers.user(), TestUsers.admin()), 0, 10, 2));

        client.get().uri("/api/admin/users?q=test&role=USER&active=true")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.totalElements").isEqualTo(2)
                .jsonPath("$.data.content[0].email").isEqualTo("usuario@test.com")
                .jsonPath("$.data.content[0].roleName").isEqualTo("Usuario")
                .jsonPath("$.data.content[0].password").doesNotExist()
                .jsonPath("$.data.content[0].passwordHash").doesNotExist();
    }

    @Test
    @DisplayName("crear → 201")
    void create() {
        when(useCase.create(any())).thenReturn(TestUsers.user());

        send("POST", "/api/admin/users", TestUsers.admin(), VALID_USER)
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.data.id").isEqualTo(1);
    }

    @Test
    @DisplayName("crear con datos inválidos → 400 por campo")
    void createValidation() {
        send("POST", "/api/admin/users", TestUsers.admin(),
                "{\"fullName\":\"A\",\"email\":\"malo\",\"password\":\"123\",\"role\":\"\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.data.fullName").isEqualTo("El nombre debe tener entre 3 y 120 caracteres")
                .jsonPath("$.data.email").isEqualTo("El correo no tiene un formato válido")
                .jsonPath("$.data.password").isEqualTo("La contraseña debe tener entre 6 y 72 caracteres")
                .jsonPath("$.data.role").isEqualTo("El rol es obligatorio");
    }

    @Test
    @DisplayName("correo repetido → 409")
    void duplicateEmail() {
        when(useCase.create(any())).thenThrow(new EmailAlreadyExistsException("ana@test.com"));

        send("POST", "/api/admin/users", TestUsers.admin(), VALID_USER)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("Ya existe un usuario con el correo ana@test.com");
    }

    @Test
    @DisplayName("editar con contraseña vacía la trata como 'no cambiar' y pasa el id del administrador")
    void updateBlankPassword() {
        when(useCase.update(eq(1L), any(), eq(2L))).thenReturn(TestUsers.user());

        send("PUT", "/api/admin/users/1", TestUsers.admin(),
                "{\"fullName\":\"Usuario Demo\",\"email\":\"usuario@test.com\",\"role\":\"USER\",\"password\":\"\"}")
                .expectStatus().isOk();

        verify(useCase).update(1L,
                new UpdateUserCommand("Usuario Demo", "usuario@test.com", "USER", null, null), 2L);
    }

    @Test
    @DisplayName("eliminar un usuario con créditos → 409")
    void deleteInUse() {
        doThrow(new UserInUseException()).when(useCase).delete(1L, 2L);

        client.delete().uri("/api/admin/users/1").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin()))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @DisplayName("roles con permisos")
    void roles() {
        when(useCase.findRoles()).thenReturn(List.of(TestUsers.USER_ROLE, TestUsers.ADMIN_ROLE));

        client.get().uri("/api/admin/roles").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[1].code").isEqualTo("ADMIN")
                .jsonPath("$.data[1].permissions[?(@ == 'USER_MANAGE')]").exists();
    }
}

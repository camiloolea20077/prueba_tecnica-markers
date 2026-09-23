package com.markers.data_credits.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markers.data_credits.domain.exception.EmailAlreadyExistsException;
import com.markers.data_credits.domain.exception.UserInUseException;
import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.exception.UserRuleException;
import com.markers.data_credits.domain.model.RoleCodes;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.CreateUserCommand;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.markers.data_credits.domain.port.out.PasswordEncoderPort;
import com.markers.data_credits.domain.port.out.RoleRepositoryPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.support.TestUsers;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 2L;

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private RoleRepositoryPort roleRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    private void rolesExist() {
        // lenient: cada test usa uno u otro rol
        lenient().when(roleRepository.findByCode(RoleCodes.USER)).thenReturn(Optional.of(TestUsers.USER_ROLE));
        lenient().when(roleRepository.findByCode(RoleCodes.ADMIN)).thenReturn(Optional.of(TestUsers.ADMIN_ROLE));
    }

    // ---------- Crear ----------

    @Test
    @DisplayName("crear normaliza nombre y correo y guarda la contraseña cifrada")
    void createOk() {
        rolesExist();
        when(userRepository.existsByEmail("ana@test.com", null)).thenReturn(false);
        when(passwordEncoder.encode("secreta1")).thenReturn("$2a$hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User created = service.create(new CreateUserCommand("  Ana   María  ", "  ANA@Test.com ", "secreta1",
                "USER", null));

        assertThat(created.fullName()).isEqualTo("Ana María");
        assertThat(created.email()).isEqualTo("ana@test.com");
        assertThat(created.passwordHash()).isEqualTo("$2a$hash");
        assertThat(created.active()).isTrue();
        assertThat(created.role().code()).isEqualTo(RoleCodes.USER);
    }

    @Test
    @DisplayName("crear con correo existente → 409 y no guarda")
    void createDuplicateEmail() {
        when(userRepository.existsByEmail("usuario@test.com", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("Otro", "usuario@test.com", "secreta1", "USER", true)))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear con rol inexistente → 422")
    void createUnknownRole() {
        when(roleRepository.findByCode("SUPER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateUserCommand("Otro", "otro@test.com", "secreta1",
                "SUPER", true)))
                .isInstanceOf(UserRuleException.class).hasMessageContaining("SUPER");
    }

    // ---------- Editar ----------

    @Test
    @DisplayName("editar sin contraseña conserva la actual; con contraseña la cifra")
    void updatePasswordOptional() {
        rolesExist();
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User same = service.update(1L, new UpdateUserCommand("Usuario Demo", "usuario@test.com", "USER", true, ""),
                ADMIN_ID);
        assertThat(same.passwordHash()).isEqualTo(TestUsers.PASSWORD_HASH);

        when(passwordEncoder.encode("nueva123")).thenReturn("$2a$nueva");
        User changed = service.update(1L,
                new UpdateUserCommand("Usuario Demo", "usuario@test.com", "USER", true, "nueva123"), ADMIN_ID);
        assertThat(changed.passwordHash()).isEqualTo("$2a$nueva");
        assertThat(changed.createdAt()).isEqualTo(TestUsers.CREATED_AT);
    }

    @Test
    @DisplayName("un administrador no puede quitarse su propio rol ni desactivarse")
    void updateSelfProtection() {
        rolesExist();
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestUsers.admin()));

        assertThatThrownBy(() -> service.update(ADMIN_ID,
                new UpdateUserCommand("Administrador", "admin@test.com", "USER", true, null), ADMIN_ID))
                .isInstanceOf(UserRuleException.class).hasMessageContaining("propio rol");
        assertThatThrownBy(() -> service.update(ADMIN_ID,
                new UpdateUserCommand("Administrador", "admin@test.com", "ADMIN", false, null), ADMIN_ID))
                .isInstanceOf(UserRuleException.class).hasMessageContaining("desactivar");
    }

    @Test
    @DisplayName("no se puede dejar el sistema sin administradores activos")
    void lastActiveAdmin() {
        rolesExist();
        User otherAdmin = new User(5L, "Otra Admin", "otra@test.com", "h", true, TestUsers.ADMIN_ROLE, null);
        when(userRepository.findById(5L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countActiveByRole(RoleCodes.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(5L,
                new UpdateUserCommand("Otra Admin", "otra@test.com", "USER", true, null), ADMIN_ID))
                .isInstanceOf(UserRuleException.class).hasMessageContaining("al menos un administrador");
        assertThatThrownBy(() -> service.changeStatus(5L, false, ADMIN_ID))
                .isInstanceOf(UserRuleException.class);
    }

    @Test
    @DisplayName("editar a un correo que usa otro usuario → 409")
    void updateDuplicateEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(userRepository.existsByEmail("admin@test.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L,
                new UpdateUserCommand("Usuario Demo", "admin@test.com", "USER", true, null), ADMIN_ID))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // ---------- Estado y eliminación ----------

    @Test
    @DisplayName("desactivar un usuario guarda el nuevo estado")
    void deactivate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.changeStatus(1L, false, ADMIN_ID);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().active()).isFalse();
        assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("no se elimina un usuario con créditos (409) ni el propio usuario (422)")
    void deleteRules() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(userRepository.hasCredits(1L)).thenReturn(true);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestUsers.admin()));

        assertThatThrownBy(() -> service.delete(1L, ADMIN_ID)).isInstanceOf(UserInUseException.class);
        assertThatThrownBy(() -> service.delete(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(UserRuleException.class).hasMessageContaining("propio");
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("eliminar un usuario sin créditos")
    void deleteOk() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user()));
        when(userRepository.hasCredits(1L)).thenReturn(false);

        service.delete(1L, ADMIN_ID);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("usuario inexistente → 404")
    void notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(UserNotFoundException.class);
    }
}

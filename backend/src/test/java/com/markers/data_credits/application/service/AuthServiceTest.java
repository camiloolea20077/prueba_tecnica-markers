package com.markers.data_credits.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markers.data_credits.domain.exception.InactiveUserException;
import com.markers.data_credits.domain.exception.InvalidCredentialsException;
import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.model.AuthSession;
import com.markers.data_credits.domain.model.AuthToken;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.AuthenticateUseCase.LoginCommand;
import com.markers.data_credits.domain.port.out.PasswordEncoderPort;
import com.markers.data_credits.domain.port.out.TokenProviderPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;
import com.markers.data_credits.support.TestUsers;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenProviderPort tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login correcto emite token y devuelve el usuario con su rol")
    void loginSuccess() {
        User admin = TestUsers.admin();
        AuthToken token = new AuthToken("jwt", Instant.now().plusSeconds(3600));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("123", TestUsers.PASSWORD_HASH)).thenReturn(true);
        when(tokenProvider.generate(admin)).thenReturn(token);

        AuthSession session = authService.login(new LoginCommand("  admin@test.com ", "123"));

        assertThat(session.token()).isEqualTo(token);
        assertThat(session.user().role().isAdmin()).isTrue();
    }

    @Test
    @DisplayName("contraseña incorrecta → InvalidCredentialsException y no emite token")
    void loginWrongPassword() {
        when(userRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(TestUsers.user()));
        when(passwordEncoder.matches("mala", TestUsers.PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("usuario@test.com", "mala")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenProvider, never()).generate(any());
    }

    @Test
    @DisplayName("correo inexistente → mismo error genérico que contraseña incorrecta")
    void loginUnknownEmail() {
        when(userRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("nadie@test.com", "123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("usuario inactivo con credenciales correctas → InactiveUserException")
    void loginInactiveUser() {
        when(userRepository.findByEmail("inactivo@test.com")).thenReturn(Optional.of(TestUsers.inactiveUser()));
        when(passwordEncoder.matches("123", TestUsers.PASSWORD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginCommand("inactivo@test.com", "123")))
                .isInstanceOf(InactiveUserException.class);
        verify(tokenProvider, never()).generate(any());
    }

    @Test
    @DisplayName("getCurrentUser devuelve rol y permisos vigentes")
    void currentUser() {
        when(userRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(TestUsers.user()));

        User user = authService.getCurrentUser("usuario@test.com");

        assertThat(user.role().permissions()).contains("CREDIT_REQUEST").doesNotContain("CREDIT_APPROVE");
    }

    @Test
    @DisplayName("getCurrentUser con usuario eliminado → UserNotFoundException")
    void currentUserNotFound() {
        when(userRepository.findByEmail("borrado@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("borrado@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}

package com.markers.data_credits.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.exception.InactiveUserException;
import com.markers.data_credits.domain.exception.InvalidCredentialsException;
import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.model.AuthSession;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.AuthenticateUseCase;
import com.markers.data_credits.domain.port.in.GetCurrentUserUseCase;
import com.markers.data_credits.domain.port.out.PasswordEncoderPort;
import com.markers.data_credits.domain.port.out.TokenProviderPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;

/**
 * Autenticación de usuarios y consulta del usuario en sesión.
 * <p>
 * Métodos síncronos (JPA). La capa web los ejecuta en {@code boundedElastic}.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AuthService implements AuthenticateUseCase, GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public AuthService(UserRepositoryPort userRepository,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Valida correo y contraseña; si son correctos y el usuario está activo emite un JWT.
     */
    @Override
    public AuthSession login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email().trim())
                .filter(u -> passwordEncoder.matches(command.password(), u.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.active()) {
            throw new InactiveUserException();
        }
        return new AuthSession(tokenProvider.generate(user), user);
    }

    /**
     * Recarga el usuario desde base de datos para devolver rol y permisos vigentes.
     */
    @Override
    public User getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        if (!user.active()) {
            throw new InactiveUserException();
        }
        return user;
    }
}

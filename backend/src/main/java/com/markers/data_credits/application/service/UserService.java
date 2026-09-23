package com.markers.data_credits.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.markers.data_credits.domain.exception.EmailAlreadyExistsException;
import com.markers.data_credits.domain.exception.UserInUseException;
import com.markers.data_credits.domain.exception.UserNotFoundException;
import com.markers.data_credits.domain.exception.UserRuleException;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.RoleCodes;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.ManageUsersUseCase;
import com.markers.data_credits.domain.port.out.PasswordEncoderPort;
import com.markers.data_credits.domain.port.out.RoleRepositoryPort;
import com.markers.data_credits.domain.port.out.UserRepositoryPort;

/**
 * CRUD de usuarios para el administrador.
 */
@Service
@Transactional(readOnly = true)
public class UserService implements ManageUsersUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordEncoderPort passwordEncoder;

    public UserService(UserRepositoryPort userRepository,
                       RoleRepositoryPort roleRepository,
                       PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<User> search(UserSearch s) {
        String query = s.query() == null || s.query().isBlank() ? null : s.query().trim();
        String role = s.roleCode() == null || s.roleCode().isBlank() ? null : s.roleCode().trim();
        return userRepository.search(query, role, s.active(), Math.max(s.page(), 0),
                Math.clamp(s.size(), 1, MAX_PAGE_SIZE));
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(String.valueOf(id)));
    }

    @Override
    @Transactional
    public User create(CreateUserCommand c) {
        String email = User.normalizeEmail(c.email());
        requireEmailAvailable(email, null);
        Role role = requireRole(c.roleCode());
        return userRepository.save(User.create(c.fullName(), email, passwordEncoder.encode(c.password()), role,
                c.isActive()));
    }

    @Override
    @Transactional
    public User update(Long id, UpdateUserCommand c, Long actorId) {
        User current = findById(id);
        String email = User.normalizeEmail(c.email());
        requireEmailAvailable(email, id);
        Role role = requireRole(c.roleCode());

        if (isSelf(id, actorId)) {
            if (!role.code().equals(current.role().code())) {
                throw new UserRuleException("No puedes cambiar tu propio rol");
            }
            if (!c.isActive()) {
                throw new UserRuleException("No puedes desactivar tu propio usuario");
            }
        }
        boolean losesAdmin = current.isAdmin() && current.active() && (!role.isAdmin() || !c.isActive());
        if (losesAdmin) {
            requireAnotherActiveAdmin();
        }

        String newHash = c.newPassword() == null || c.newPassword().isBlank()
                ? null
                : passwordEncoder.encode(c.newPassword());
        return userRepository.save(current.withChanges(c.fullName(), email, role, c.isActive(), newHash));
    }

    @Override
    @Transactional
    public User changeStatus(Long id, boolean active, Long actorId) {
        User current = findById(id);
        if (!active && isSelf(id, actorId)) {
            throw new UserRuleException("No puedes desactivar tu propio usuario");
        }
        if (!active && current.isAdmin() && current.active()) {
            requireAnotherActiveAdmin();
        }
        return current.active() == active ? current : userRepository.save(current.withActive(active));
    }

    @Override
    @Transactional
    public void delete(Long id, Long actorId) {
        User current = findById(id);
        if (isSelf(id, actorId)) {
            throw new UserRuleException("No puedes eliminar tu propio usuario");
        }
        if (userRepository.hasCredits(id)) {
            throw new UserInUseException();
        }
        if (current.isAdmin() && current.active()) {
            requireAnotherActiveAdmin();
        }
        userRepository.deleteById(id);
    }

    @Override
    public List<Role> findRoles() {
        return roleRepository.findAll();
    }

    private void requireEmailAvailable(String email, Long excludeId) {
        if (userRepository.existsByEmail(email, excludeId)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private Role requireRole(String code) {
        return roleRepository.findByCode(code == null ? "" : code.trim())
                .orElseThrow(() -> new UserRuleException("El rol " + code + " no existe"));
    }

    /** El sistema nunca debe quedar sin un administrador activo. */
    private void requireAnotherActiveAdmin() {
        if (userRepository.countActiveByRole(RoleCodes.ADMIN) <= 1) {
            throw new UserRuleException("Debe quedar al menos un administrador activo");
        }
    }

    private static boolean isSelf(Long id, Long actorId) {
        return Objects.equals(id, actorId);
    }
}

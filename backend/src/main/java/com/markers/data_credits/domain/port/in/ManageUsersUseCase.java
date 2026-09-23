package com.markers.data_credits.domain.port.in;

import java.util.List;

import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.Role;
import com.markers.data_credits.domain.model.User;

/**
 * Caso de uso del administrador: CRUD de usuarios y consulta de roles.
 * <p>
 * Reglas: correo único; nadie se desactiva, se elimina ni se cambia el rol a sí mismo;
 * siempre debe quedar al menos un administrador activo; no se elimina un usuario con créditos.
 * </p>
 */
public interface ManageUsersUseCase {

    PageResult<User> search(UserSearch search);

    /** @throws com.markers.data_credits.domain.exception.UserNotFoundException si no existe */
    User findById(Long id);

    /**
     * @throws com.markers.data_credits.domain.exception.EmailAlreadyExistsException si el correo ya existe
     * @throws com.markers.data_credits.domain.exception.UserRuleException           si el rol no existe
     */
    User create(CreateUserCommand command);

    User update(Long id, UpdateUserCommand command, Long actorId);

    User changeStatus(Long id, boolean active, Long actorId);

    /** @throws com.markers.data_credits.domain.exception.UserInUseException si tiene créditos */
    void delete(Long id, Long actorId);

    List<Role> findRoles();

    record UserSearch(String query, String roleCode, Boolean active, int page, int size) {
    }

    record CreateUserCommand(String fullName, String email, String password, String roleCode, Boolean active) {

        public boolean isActive() {
            return active == null || active;
        }
    }

    /** {@code newPassword} {@code null} o vacío = conservar la actual. */
    record UpdateUserCommand(String fullName, String email, String roleCode, Boolean active, String newPassword) {

        public boolean isActive() {
            return active == null || active;
        }
    }
}

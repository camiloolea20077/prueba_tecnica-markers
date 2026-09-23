package com.markers.data_credits.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.markers.data_credits.domain.model.Role;

/**
 * Puerto de salida para consultar roles y sus permisos.
 */
public interface RoleRepositoryPort {

    List<Role> findAll();

    Optional<Role> findByCode(String code);
}

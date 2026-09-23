package com.markers.data_credits.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.PageResult;

/**
 * Puerto de salida para la persistencia de créditos.
 */
public interface CreditRepositoryPort {

    /**
     * Inserta o actualiza. Al actualizar compara la versión del crédito con la almacenada:
     * si difieren (otro usuario lo modificó) lanza una excepción de bloqueo optimista (HTTP 409).
     */
    Credit save(Credit credit);

    Optional<Credit> findById(Long id);

    /** {@code status} opcional; orden: más reciente primero. */
    List<Credit> findByUserId(Long userId, CreditStatus status);

    long countByUserIdAndStatus(Long userId, CreditStatus status);

    /** Búsqueda paginada por estado y texto (nombre/correo del solicitante), más reciente primero. */
    PageResult<Credit> search(CreditStatus status, String query, int page, int size);

    Map<CreditStatus, Long> countByStatus();
}

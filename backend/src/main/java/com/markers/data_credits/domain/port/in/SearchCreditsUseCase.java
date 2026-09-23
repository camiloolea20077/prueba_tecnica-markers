package com.markers.data_credits.domain.port.in;

import java.util.Map;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.PageResult;

/**
 * Caso de uso del analista: consultar todas las solicitudes.
 */
public interface SearchCreditsUseCase {

    /** Búsqueda paginada, más reciente primero. {@code status} y {@code query} son opcionales. */
    PageResult<Credit> search(CreditSearch search);

    /** Cantidad de créditos por estado (todos los estados, con 0 si no hay). */
    Map<CreditStatus, Long> countByStatus();

    /**
     * @param query texto a buscar en nombre o correo del solicitante
     * @param page  página base 0
     */
    record CreditSearch(CreditStatus status, String query, int page, int size) {
    }
}

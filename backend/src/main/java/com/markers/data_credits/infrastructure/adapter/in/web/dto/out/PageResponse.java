package com.markers.data_credits.infrastructure.adapter.in.web.dto.out;

import java.util.List;

/**
 * Página de resultados.
 *
 * @param page página actual (base 0)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}

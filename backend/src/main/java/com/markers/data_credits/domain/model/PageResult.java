package com.markers.data_credits.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados independiente de Spring Data.
 *
 * @param page número de página (base 0)
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}

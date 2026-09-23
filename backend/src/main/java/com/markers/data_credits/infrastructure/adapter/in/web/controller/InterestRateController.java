package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.port.in.QueryInterestRatesUseCase;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.RateCatalogResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.CreditWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;

import reactor.core.publisher.Mono;

/**
 * Tramos de tasa EA vigentes (cualquier usuario autenticado).
 */
@RestController
@RequestMapping("/api/interest-rates")
public class InterestRateController {

    private final QueryInterestRatesUseCase queryInterestRatesUseCase;
    private final CreditWebMapper mapper;
    private final BlockingExecutor executor;

    public InterestRateController(QueryInterestRatesUseCase queryInterestRatesUseCase,
                                  CreditWebMapper mapper,
                                  BlockingExecutor executor) {
        this.queryInterestRatesUseCase = queryInterestRatesUseCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    /** {@code GET /api/interest-rates} → tramos activos + tasa EA mínima/máxima. */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<RateCatalogResponse>>> catalog() {
        return executor.run(queryInterestRatesUseCase::getCatalog)
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Tramos de tasa vigentes", body)));
    }
}

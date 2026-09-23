package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.model.Permissions;
import com.markers.data_credits.domain.port.in.ManageInterestRatesUseCase;
import com.markers.data_credits.domain.port.in.ManageInterestRatesUseCase.TierCommand;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.InterestRateTierRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.InterestRateTierResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.CreditWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * CRUD de tramos de tasa efectiva anual (rol ADMIN + permiso RATE_MANAGE).
 */
@RestController
@RequestMapping("/api/admin/interest-rates")
public class AdminInterestRateController {

    private static final String CAN_MANAGE = "hasAuthority('" + Permissions.RATE_MANAGE + "')";

    private final ManageInterestRatesUseCase useCase;
    private final CreditWebMapper mapper;
    private final BlockingExecutor executor;

    public AdminInterestRateController(ManageInterestRatesUseCase useCase, CreditWebMapper mapper,
                                       BlockingExecutor executor) {
        this.useCase = useCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    @GetMapping
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<List<InterestRateTierResponse>>>> findAll() {
        return executor.run(useCase::findAll)
                .map(mapper::toTierResponses)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Tramos de tasa", body)));
    }

    /** @return 201; 400 formato; 422 rango inválido o superpuesto con otro tramo activo */
    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<InterestRateTierResponse>>> create(
            @Valid @RequestBody InterestRateTierRequest request) {
        return executor.run(() -> useCase.create(toCommand(request)))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.of(HttpStatus.CREATED, "Tramo creado", body)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<InterestRateTierResponse>>> update(
            @PathVariable Long id, @Valid @RequestBody InterestRateTierRequest request) {
        return executor.run(() -> useCase.update(id, toCommand(request)))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Tramo actualizado", body)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public Mono<ResponseEntity<ApiResponse<Object>>> delete(@PathVariable Long id) {
        return executor.run(() -> {
                    useCase.delete(id);
                    return ApiResponse.ok("Tramo eliminado", null);
                })
                .map(ResponseEntity::ok);
    }

    private static TierCommand toCommand(InterestRateTierRequest r) {
        return new TierCommand(r.name(), r.minTermMonths(), r.maxTermMonths(), r.annualEffectiveRate(), r.active());
    }
}

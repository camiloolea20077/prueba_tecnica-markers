package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.Permissions;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase.ApproveCreditCommand;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase.RejectCreditCommand;
import com.markers.data_credits.domain.port.in.SearchCreditsUseCase;
import com.markers.data_credits.domain.port.in.SearchCreditsUseCase.CreditSearch;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.ApproveCreditRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.RejectCreditRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditSummaryResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.PageResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.CreditWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;
import com.markers.data_credits.infrastructure.security.AuthenticatedUser;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * Administración de créditos: listar solicitudes y aprobarlas o rechazarlas.
 * Doble barrera: {@code /api/admin/**} exige rol ADMIN y cada método su permiso.
 */
@RestController
@RequestMapping("/api/admin/credits")
public class AdminCreditController {

    private final SearchCreditsUseCase searchCreditsUseCase;
    private final DecideCreditUseCase decideCreditUseCase;
    private final CreditWebMapper mapper;
    private final BlockingExecutor executor;

    public AdminCreditController(SearchCreditsUseCase searchCreditsUseCase,
                                 DecideCreditUseCase decideCreditUseCase,
                                 CreditWebMapper mapper,
                                 BlockingExecutor executor) {
        this.searchCreditsUseCase = searchCreditsUseCase;
        this.decideCreditUseCase = decideCreditUseCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    /**
     * Solicitudes paginadas. {@code GET /api/admin/credits?status=PENDING&q=ana&page=0&size=10}
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_VIEW_ALL + "')")
    public Mono<ResponseEntity<ApiResponse<PageResponse<CreditResponse>>>> search(
            @RequestParam(required = false) CreditStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CreditSearch search = new CreditSearch(status, q, page, size);
        return executor.run(() -> searchCreditsUseCase.search(search))
                .map(mapper::toPageResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Solicitudes de crédito", body)));
    }

    /** Conteo por estado. {@code GET /api/admin/credits/summary} */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_VIEW_ALL + "')")
    public Mono<ResponseEntity<ApiResponse<CreditSummaryResponse>>> summary() {
        return executor.run(searchCreditsUseCase::countByStatus)
                .map(mapper::toSummary)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Resumen de solicitudes", body)));
    }

    /**
     * Aprueba con la tasa EA indicada. {@code PATCH /api/admin/credits/{id}/approve}
     *
     * @return 200; 404 no existe; 409 ya decidido o modificado en paralelo; 422 tasa fuera de rango
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_APPROVE + "')")
    public Mono<ResponseEntity<ApiResponse<CreditResponse>>> approve(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody ApproveCreditRequest request) {
        ApproveCreditCommand command = new ApproveCreditCommand(id, principal.id(), request.annualRate());
        return executor.run(() -> decideCreditUseCase.approve(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Crédito aprobado", body)));
    }

    /**
     * Rechaza con motivo. {@code PATCH /api/admin/credits/{id}/reject}
     *
     * @return 200; 400 motivo inválido; 404 no existe; 409 ya decidido
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_REJECT + "')")
    public Mono<ResponseEntity<ApiResponse<CreditResponse>>> reject(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody RejectCreditRequest request) {
        RejectCreditCommand command = new RejectCreditCommand(id, principal.id(), request.reason());
        return executor.run(() -> decideCreditUseCase.reject(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Crédito rechazado", body)));
    }
}

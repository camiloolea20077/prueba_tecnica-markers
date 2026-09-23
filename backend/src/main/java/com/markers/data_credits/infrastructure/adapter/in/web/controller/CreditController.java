package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.Permissions;
import com.markers.data_credits.domain.port.in.CancelCreditUseCase;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase.Requester;
import com.markers.data_credits.domain.port.in.RequestCreditUseCase;
import com.markers.data_credits.domain.port.in.RequestCreditUseCase.RequestCreditCommand;
import com.markers.data_credits.domain.port.in.SimulateCreditUseCase;
import com.markers.data_credits.domain.port.in.SimulateCreditUseCase.SimulateCreditCommand;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.CreateCreditRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.in.SimulateCreditRequest;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditQuoteResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.mapper.CreditWebMapper;
import com.markers.data_credits.infrastructure.adapter.in.web.support.BlockingExecutor;
import com.markers.data_credits.infrastructure.exception.ApiResponse;
import com.markers.data_credits.infrastructure.security.AuthenticatedUser;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * Créditos del solicitante: solicitar, simular, consultar y cancelar.
 * Todos los métodos devuelven {@code Mono} (requisito de {@code @PreAuthorize} reactivo).
 */
@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final RequestCreditUseCase requestCreditUseCase;
    private final SimulateCreditUseCase simulateCreditUseCase;
    private final QueryCreditUseCase queryCreditUseCase;
    private final CancelCreditUseCase cancelCreditUseCase;
    private final CreditWebMapper mapper;
    private final BlockingExecutor executor;

    public CreditController(RequestCreditUseCase requestCreditUseCase,
                            SimulateCreditUseCase simulateCreditUseCase,
                            QueryCreditUseCase queryCreditUseCase,
                            CancelCreditUseCase cancelCreditUseCase,
                            CreditWebMapper mapper,
                            BlockingExecutor executor) {
        this.requestCreditUseCase = requestCreditUseCase;
        this.simulateCreditUseCase = simulateCreditUseCase;
        this.queryCreditUseCase = queryCreditUseCase;
        this.cancelCreditUseCase = cancelCreditUseCase;
        this.mapper = mapper;
        this.executor = executor;
    }

    /**
     * Solicita un crédito. {@code POST /api/credits}
     *
     * @return 201 con el crédito en estado PENDING y la cuota estimada; 400 formato inválido;
     *         422 monto/plazo fuera de rango o demasiadas solicitudes pendientes
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_REQUEST + "')")
    public Mono<ResponseEntity<ApiResponse<CreditResponse>>> request(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateCreditRequest request) {
        RequestCreditCommand command = new RequestCreditCommand(principal.id(), request.amount(), request.termMonths());
        return executor.run(() -> requestCreditUseCase.request(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.of(HttpStatus.CREATED, "Solicitud de crédito registrada", body)));
    }

    /**
     * Simula la cuota sin guardar nada. {@code POST /api/credits/simulate}
     */
    @PostMapping("/simulate")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_SIMULATE + "')")
    public Mono<ResponseEntity<ApiResponse<CreditQuoteResponse>>> simulate(
            @Valid @RequestBody SimulateCreditRequest request) {
        SimulateCreditCommand command = new SimulateCreditCommand(
                request.amount(), request.termMonths(), request.annualRate(), request.scheduleRequested());
        return executor.run(() -> simulateCreditUseCase.simulate(command))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Simulación calculada", body)));
    }

    /**
     * Créditos del usuario autenticado. {@code GET /api/credits/me?status=PENDING}
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_VIEW_OWN + "')")
    public Mono<ResponseEntity<ApiResponse<List<CreditResponse>>>> myCredits(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) CreditStatus status) {
        return executor.run(() -> queryCreditUseCase.findByUser(principal.id(), status))
                .map(mapper::toResponses)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Créditos del usuario", body)));
    }

    /**
     * Detalle y estado de un crédito (dueño o administrador). {@code GET /api/credits/{id}}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + Permissions.CREDIT_VIEW_OWN + "', '" + Permissions.CREDIT_VIEW_ALL + "')")
    public Mono<ResponseEntity<ApiResponse<CreditResponse>>> findById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        Requester requester = new Requester(principal.id(),
                principal.permissions().contains(Permissions.CREDIT_VIEW_ALL));
        return executor.run(() -> queryCreditUseCase.findById(id, requester))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Detalle del crédito", body)));
    }

    /**
     * El dueño cancela su solicitud pendiente. {@code PATCH /api/credits/{id}/cancel}
     *
     * @return 200; 404 si no existe o no es suyo; 409 si ya no está pendiente
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permissions.CREDIT_CANCEL_OWN + "')")
    public Mono<ResponseEntity<ApiResponse<CreditResponse>>> cancel(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        return executor.run(() -> cancelCreditUseCase.cancel(id, principal.id()))
                .map(mapper::toResponse)
                .map(body -> ResponseEntity.ok(ApiResponse.ok("Solicitud cancelada", body)));
    }
}

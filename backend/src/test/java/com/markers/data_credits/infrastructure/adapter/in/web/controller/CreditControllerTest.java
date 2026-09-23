package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.markers.data_credits.domain.exception.CreditNotFoundException;
import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.RateCatalog;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.CancelCreditUseCase;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase;
import com.markers.data_credits.domain.port.in.QueryCreditUseCase.Requester;
import com.markers.data_credits.domain.port.in.QueryInterestRatesUseCase;
import com.markers.data_credits.domain.port.in.RequestCreditUseCase;
import com.markers.data_credits.domain.port.in.SimulateCreditUseCase;
import com.markers.data_credits.domain.service.InterestCalculator;
import com.markers.data_credits.infrastructure.security.JwtTokenProvider;
import com.markers.data_credits.support.TestCredits;
import com.markers.data_credits.support.TestUsers;
import com.markers.data_credits.support.WebLayerTest;

/**
 * Endpoints de créditos del solicitante: permisos, códigos HTTP y forma de la respuesta.
 */
@WebFluxTest(controllers = {CreditController.class, InterestRateController.class})
@WebLayerTest
class CreditControllerTest {

    @Autowired
    private WebTestClient client;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private RequestCreditUseCase requestCreditUseCase;
    @MockitoBean
    private SimulateCreditUseCase simulateCreditUseCase;
    @MockitoBean
    private QueryCreditUseCase queryCreditUseCase;
    @MockitoBean
    private CancelCreditUseCase cancelCreditUseCase;
    @MockitoBean
    private QueryInterestRatesUseCase queryInterestRatesUseCase;

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generate(user).value();
    }

    private WebTestClient.ResponseSpec post(String uri, User user, String body) {
        return client.post().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    // ---------- POST /api/credits ----------

    @Test
    @DisplayName("USER solicita crédito → 201 PENDIENTE con cuota estimada")
    void requestCreated() {
        when(requestCreditUseCase.request(any())).thenReturn(TestCredits.credit(10L, CreditStatus.PENDING));

        post("/api/credits", TestUsers.user(), "{\"amount\":10000000,\"termMonths\":24}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.data.id").isEqualTo(10)
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.statusLabel").isEqualTo("Pendiente")
                .jsonPath("$.data.estimated").isEqualTo(true)
                .jsonPath("$.data.annualRate").isEqualTo(22.0)
                .jsonPath("$.data.monthlyPayment").isNumber()
                .jsonPath("$.data.applicant.email").isEqualTo("usuario@test.com");
    }

    @Test
    @DisplayName("ADMIN no tiene CREDIT_REQUEST → 403 y no se llama al caso de uso")
    void requestForbiddenForAdmin() {
        post("/api/credits", TestUsers.admin(), "{\"amount\":10000000,\"termMonths\":24}")
                .expectStatus().isForbidden();
        verifyNoInteractions(requestCreditUseCase);
    }

    @Test
    @DisplayName("sin token → 401")
    void requestUnauthenticated() {
        client.post().uri("/api/credits").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":10000000,\"termMonths\":24}").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("formato inválido → 400 con error por campo")
    void requestValidation() {
        post("/api/credits", TestUsers.user(), "{\"amount\":-5,\"termMonths\":null}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.data.amount").isEqualTo("El monto debe ser mayor a cero")
                .jsonPath("$.data.termMonths").isEqualTo("El plazo es obligatorio");
    }

    @Test
    @DisplayName("regla de negocio incumplida → 422 con el mensaje del dominio")
    void requestBusinessRule() {
        when(requestCreditUseCase.request(any()))
                .thenThrow(new CreditRuleException("El plazo debe estar entre 6 y 84 meses"));

        post("/api/credits", TestUsers.user(), "{\"amount\":10000000,\"termMonths\":100}")
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.message").isEqualTo("El plazo debe estar entre 6 y 84 meses");
    }

    // ---------- Consultas ----------

    @Test
    @DisplayName("GET /me devuelve los créditos del usuario del token")
    void myCredits() {
        when(queryCreditUseCase.findByUser(1L, CreditStatus.PENDING))
                .thenReturn(List.of(TestCredits.credit(10L, CreditStatus.PENDING)));

        client.get().uri("/api/credits/me?status=PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].status").isEqualTo("PENDING");
    }

    @Test
    @DisplayName("GET /me con estado inexistente → 400")
    void myCreditsInvalidStatus() {
        client.get().uri("/api/credits/me?status=FOO")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /{id}: el admin consulta con permiso de ver todos")
    void findByIdAsAdmin() {
        when(queryCreditUseCase.findById(eq(10L), eq(new Requester(2L, true))))
                .thenReturn(TestCredits.credit(10L, CreditStatus.PENDING));

        client.get().uri("/api/credits/10")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /{id} ajeno o inexistente → 404")
    void findByIdNotFound() {
        when(queryCreditUseCase.findById(eq(10L), any())).thenThrow(new CreditNotFoundException(10L));

        client.get().uri("/api/credits/10")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("No existe el crédito 10");
    }

    // ---------- Cancelar ----------

    @Test
    @DisplayName("PATCH /{id}/cancel sobre un crédito ya decidido → 409")
    void cancelConflict() {
        when(cancelCreditUseCase.cancel(10L, 1L))
                .thenThrow(new InvalidCreditStateException(CreditStatus.APPROVED, "cancelar"));

        client.patch().uri("/api/credits/10/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("No se puede cancelar un crédito en estado aprobado");
    }

    @Test
    @DisplayName("PATCH /{id}/cancel propio y pendiente → 200 CANCELADO")
    void cancelOk() {
        when(cancelCreditUseCase.cancel(10L, 1L)).thenReturn(TestCredits.credit(10L, CreditStatus.CANCELLED));

        client.patch().uri("/api/credits/10/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.status").isEqualTo("CANCELLED");
    }

    // ---------- Simulación y tasas ----------

    @Test
    @DisplayName("POST /simulate → 200 con cuota y tabla de amortización (USER y ADMIN)")
    void simulate() {
        when(simulateCreditUseCase.simulate(any()))
                .thenReturn(InterestCalculator.quote(new BigDecimal("10000000"), 12, new BigDecimal("24"), true));

        for (User user : List.of(TestUsers.user(), TestUsers.admin())) {
            post("/api/credits/simulate", user, "{\"amount\":10000000,\"termMonths\":12,\"annualRate\":24}")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.monthlyRate").isEqualTo(1.808758)
                    .jsonPath("$.data.schedule.length()").isEqualTo(12)
                    .jsonPath("$.data.schedule[11].balance").isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("GET /api/interest-rates → tramos y límites de tasa, monto y plazo")
    void interestRates() {
        when(queryInterestRatesUseCase.getCatalog())
                .thenReturn(RateCatalog.of(List.of(TestCredits.MEDIUM_TIER), TestCredits.POLICY));

        client.get().uri("/api/interest-rates")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user())).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.maxAnnualRate").isEqualTo(28.0)
                .jsonPath("$.data.minAmount").isEqualTo(1000000)
                .jsonPath("$.data.maxTermMonths").isEqualTo(84)
                .jsonPath("$.data.tiers[0].name").isEqualTo("Mediano plazo")
                .jsonPath("$.data.tiers[0].annualEffectiveRate").isEqualTo(22.0);
    }
}

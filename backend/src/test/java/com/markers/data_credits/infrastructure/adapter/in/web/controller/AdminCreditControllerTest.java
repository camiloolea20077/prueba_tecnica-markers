package com.markers.data_credits.infrastructure.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.markers.data_credits.domain.exception.CreditRuleException;
import com.markers.data_credits.domain.exception.InvalidCreditStateException;
import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditStatus;
import com.markers.data_credits.domain.model.InterestRateTier;
import com.markers.data_credits.domain.model.PageResult;
import com.markers.data_credits.domain.model.User;
import com.markers.data_credits.domain.port.in.DecideCreditUseCase;
import com.markers.data_credits.domain.port.in.ManageInterestRatesUseCase;
import com.markers.data_credits.domain.port.in.SearchCreditsUseCase;
import com.markers.data_credits.infrastructure.adapter.out.persistence.entity.CreditEntity;
import com.markers.data_credits.infrastructure.security.JwtTokenProvider;
import com.markers.data_credits.support.TestCredits;
import com.markers.data_credits.support.TestUsers;
import com.markers.data_credits.support.WebLayerTest;

/**
 * Endpoints de administración: rol/permisos, códigos HTTP y forma de la respuesta.
 */
@WebFluxTest(controllers = {AdminCreditController.class, AdminInterestRateController.class})
@WebLayerTest
class AdminCreditControllerTest {

    @Autowired
    private WebTestClient client;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private SearchCreditsUseCase searchCreditsUseCase;
    @MockitoBean
    private DecideCreditUseCase decideCreditUseCase;
    @MockitoBean
    private ManageInterestRatesUseCase manageInterestRatesUseCase;

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generate(user).value();
    }

    private WebTestClient.ResponseSpec patch(String uri, User user, String body) {
        return client.patch().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private static Credit approved() {
        return TestCredits.credit(10L, CreditStatus.PENDING).approve(2L, new BigDecimal("20"), TestCredits.POLICY);
    }

    // ---------- Seguridad ----------

    @Test
    @DisplayName("USER no puede aprobar (bloqueado por /api/admin/**) y no se llama al caso de uso")
    void userCannotApprove() {
        patch("/api/admin/credits/10/approve", TestUsers.user(), "{\"annualRate\":20}")
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isEqualTo(true);
        verifyNoInteractions(decideCreditUseCase);
    }

    @Test
    @DisplayName("USER no puede listar todas las solicitudes")
    void userCannotSearch() {
        client.get().uri("/api/admin/credits").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user()))
                .exchange().expectStatus().isForbidden();
    }

    // ---------- Aprobar ----------

    @Test
    @DisplayName("ADMIN aprueba → 200 con condiciones definitivas (no estimadas)")
    void approveOk() {
        when(decideCreditUseCase.approve(any())).thenReturn(approved());

        patch("/api/admin/credits/10/approve", TestUsers.admin(), "{\"annualRate\":20}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Crédito aprobado")
                .jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.estimated").isEqualTo(false)
                .jsonPath("$.data.annualRate").isEqualTo(20.0)
                .jsonPath("$.data.decidedAt").isNotEmpty();
    }

    @Test
    @DisplayName("aprobar sin tasa → 400")
    void approveWithoutRate() {
        patch("/api/admin/credits/10/approve", TestUsers.admin(), "{}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.data.annualRate").isEqualTo("La tasa efectiva anual es obligatoria");
    }

    @Test
    @DisplayName("aprobar con tasa fuera de rango → 422")
    void approveRateOutOfRange() {
        when(decideCreditUseCase.approve(any()))
                .thenThrow(new CreditRuleException("La tasa efectiva anual debe estar entre 10 % y 28 %"));

        patch("/api/admin/credits/10/approve", TestUsers.admin(), "{\"annualRate\":35}")
                .expectStatus().isEqualTo(422);
    }

    @Test
    @DisplayName("aprobar un crédito ya decidido → 409")
    void approveAlreadyDecided() {
        when(decideCreditUseCase.approve(any()))
                .thenThrow(new InvalidCreditStateException(CreditStatus.REJECTED, "aprobar"));

        patch("/api/admin/credits/10/approve", TestUsers.admin(), "{\"annualRate\":20}")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("No se puede aprobar un crédito en estado rechazado");
    }

    @Test
    @DisplayName("dos analistas decidiendo a la vez (bloqueo optimista) → 409")
    void concurrentDecision() {
        when(decideCreditUseCase.approve(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(CreditEntity.class, 10L));

        patch("/api/admin/credits/10/approve", TestUsers.admin(), "{\"annualRate\":20}")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message")
                .isEqualTo("El registro fue modificado por otro usuario. Actualice e intente de nuevo");
    }

    // ---------- Rechazar ----------

    @Test
    @DisplayName("rechazar con motivo corto → 400")
    void rejectShortReason() {
        patch("/api/admin/credits/10/reject", TestUsers.admin(), "{\"reason\":\"no\"}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.data.reason").isEqualTo("El motivo debe tener entre 10 y 500 caracteres");
    }

    @Test
    @DisplayName("rechazar → 200 con el motivo")
    void rejectOk() {
        when(decideCreditUseCase.reject(any())).thenReturn(
                TestCredits.credit(10L, CreditStatus.PENDING).reject(2L, "Capacidad de pago insuficiente"));

        patch("/api/admin/credits/10/reject", TestUsers.admin(), "{\"reason\":\"Capacidad de pago insuficiente\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("REJECTED")
                .jsonPath("$.data.rejectionReason").isEqualTo("Capacidad de pago insuficiente");
    }

    // ---------- Consultas ----------

    @Test
    @DisplayName("listar → página con metadatos y solicitante")
    void searchPage() {
        when(searchCreditsUseCase.search(any())).thenReturn(
                new PageResult<>(List.of(TestCredits.credit(10L, CreditStatus.PENDING)), 0, 10, 21));

        client.get().uri("/api/admin/credits?status=PENDING&q=usuario&page=0&size=10")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content[0].applicant.email").isEqualTo("usuario@test.com")
                .jsonPath("$.data.totalElements").isEqualTo(21)
                .jsonPath("$.data.totalPages").isEqualTo(3);
    }

    @Test
    @DisplayName("resumen por estado")
    void summary() {
        when(searchCreditsUseCase.countByStatus()).thenReturn(Map.of(
                CreditStatus.PENDING, 3L, CreditStatus.APPROVED, 2L,
                CreditStatus.REJECTED, 1L, CreditStatus.CANCELLED, 0L));

        client.get().uri("/api/admin/credits/summary")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin())).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.pending").isEqualTo(3)
                .jsonPath("$.data.total").isEqualTo(6);
    }

    // ---------- Tramos ----------

    @Test
    @DisplayName("CRUD de tramos: crear → 201; superpuesto → 422; USER → 403")
    void interestRateTiers() {
        when(manageInterestRatesUseCase.create(any()))
                .thenReturn(new InterestRateTier(4L, "Especial", 85, 90, new BigDecimal("20.0000"), true))
                .thenThrow(new CreditRuleException("El rango 13–20 meses se cruza con el tramo \"Mediano\""));
        String body = "{\"name\":\"Especial\",\"minTermMonths\":13,\"maxTermMonths\":20,\"annualEffectiveRate\":20}";

        client.post().uri("/api/admin/interest-rates").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin()))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.data.id").isEqualTo(4);

        client.post().uri("/api/admin/interest-rates").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.admin()))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange()
                .expectStatus().isEqualTo(422);

        client.post().uri("/api/admin/interest-rates").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.user()))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange()
                .expectStatus().isForbidden();
    }
}

package com.markers.data_credits.infrastructure.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.markers.data_credits.domain.model.Credit;
import com.markers.data_credits.domain.model.CreditQuote;
import com.markers.data_credits.domain.model.RateCatalog;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditQuoteResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.CreditResponse;
import com.markers.data_credits.infrastructure.adapter.in.web.dto.out.RateCatalogResponse;

/**
 * Conversión dominio → DTOs web de créditos.
 */
@Mapper
public interface CreditWebMapper {

    CreditQuoteResponse toResponse(CreditQuote quote);

    RateCatalogResponse toResponse(RateCatalog catalog);

    List<CreditResponse> toResponses(List<Credit> credits);

    /** Las condiciones salen de {@link Credit#quote()}: definitivas si está aprobado, estimadas si no. */
    default CreditResponse toResponse(Credit credit) {
        CreditQuote quote = credit.quote();
        return new CreditResponse(
                credit.id(),
                credit.applicant() == null ? null : new CreditResponse.ApplicantResponse(
                        credit.applicant().id(), credit.applicant().fullName(), credit.applicant().email()),
                credit.amount(),
                credit.termMonths(),
                credit.status().name(),
                credit.status().label(),
                credit.isEstimated(),
                credit.suggestedAnnualRate(),
                quote.annualRate(),
                quote.monthlyRate(),
                quote.monthlyPayment(),
                quote.totalInterest(),
                quote.totalPayable(),
                credit.rejectionReason(),
                credit.createdAt(),
                credit.decidedAt());
    }
}

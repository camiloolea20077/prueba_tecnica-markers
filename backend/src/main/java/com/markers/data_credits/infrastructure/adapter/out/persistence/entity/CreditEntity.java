package com.markers.data_credits.infrastructure.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.markers.data_credits.domain.model.CreditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credits")
@Getter
@Setter
@NoArgsConstructor
public class CreditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "term_months", nullable = false, updatable = false)
    private Integer termMonths;

    @Column(name = "suggested_annual_rate", nullable = false, precision = 7, scale = 4, updatable = false)
    private BigDecimal suggestedAnnualRate;

    @Column(name = "annual_effective_rate", precision = 7, scale = 4)
    private BigDecimal annualEffectiveRate;

    @Column(name = "monthly_rate", precision = 9, scale = 6)
    private BigDecimal monthlyRate;

    @Column(name = "monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_interest", precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_payable", precision = 15, scale = 2)
    private BigDecimal totalPayable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserEntity decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Bloqueo optimista: dos decisiones simultáneas sobre el mismo crédito → la segunda falla. */
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

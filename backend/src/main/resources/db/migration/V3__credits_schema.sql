-- Tramos de tasa efectiva anual (EA) por rango de plazo y créditos

CREATE TABLE interest_rate_tiers (
    id                    BIGSERIAL    PRIMARY KEY,
    name                  VARCHAR(80)  NOT NULL,
    min_term_months       INTEGER      NOT NULL,
    max_term_months       INTEGER      NOT NULL,
    annual_effective_rate NUMERIC(7,4) NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_tiers_term_range CHECK (min_term_months > 0 AND max_term_months >= min_term_months),
    CONSTRAINT ck_tiers_rate CHECK (annual_effective_rate > 0)
);

CREATE TABLE credits (
    id                    BIGSERIAL     PRIMARY KEY,
    user_id               BIGINT        NOT NULL REFERENCES users (id),
    amount                NUMERIC(15,2) NOT NULL,
    term_months           INTEGER       NOT NULL,
    -- Tasa sugerida por el tramo al momento de solicitar (%)
    suggested_annual_rate NUMERIC(7,4)  NOT NULL,
    -- Condiciones definitivas: se llenan al aprobar
    annual_effective_rate NUMERIC(7,4),
    monthly_rate          NUMERIC(9,6),
    monthly_payment       NUMERIC(15,2),
    total_interest        NUMERIC(15,2),
    total_payable         NUMERIC(15,2),
    status                VARCHAR(20)   NOT NULL,
    rejection_reason      VARCHAR(500),
    decided_by            BIGINT        REFERENCES users (id),
    decided_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_credits_amount CHECK (amount > 0),
    CONSTRAINT ck_credits_term CHECK (term_months > 0),
    CONSTRAINT ck_credits_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX ix_credits_user_status ON credits (user_id, status);
CREATE INDEX ix_credits_status_created ON credits (status, created_at DESC);

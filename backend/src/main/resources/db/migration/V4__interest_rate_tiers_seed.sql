-- Tramos iniciales de tasa efectiva anual (el admin puede ajustarlos)

INSERT INTO interest_rate_tiers (name, min_term_months, max_term_months, annual_effective_rate) VALUES
    ('Corto plazo',   6, 12, 18.0000),
    ('Mediano plazo', 13, 36, 22.0000),
    ('Largo plazo',   37, 84, 25.0000);

-- Datos de demostración: usuarios adicionales y créditos en todos los estados.
-- Password de todos los usuarios demo: 123 (BCrypt). Las condiciones de los créditos aprobados
-- se calcularon con las mismas fórmulas y redondeos de InterestCalculator.

INSERT INTO users (full_name, email, password, active, role_id)
SELECT v.full_name, v.email, '$2a$10$JUv1GoccusdSelHqWKM79OO.Kq11FVQl.LSoSNODTKxaAJIPEMeWe', v.active, r.id
FROM (VALUES
        ('María López',    'maria.lopez@test.com',    TRUE,  'USER'),
        ('Carlos Ruiz',    'carlos.ruiz@test.com',    TRUE,  'USER'),
        ('Ana Torres',     'ana.torres@test.com',     TRUE,  'USER'),
        ('Jorge Medina',   'jorge.medina@test.com',   TRUE,  'USER'),
        ('Pedro Gómez',    'pedro.gomez@test.com',    FALSE, 'USER'),
        ('Sofía Ramírez',  'sofia.ramirez@test.com',  TRUE,  'ADMIN')
     ) AS v (full_name, email, active, role_code)
JOIN roles r ON r.code = v.role_code
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE lower(u.email) = v.email);

INSERT INTO credits (user_id, amount, term_months, suggested_annual_rate,
                     annual_effective_rate, monthly_rate, monthly_payment, total_interest, total_payable,
                     status, rejection_reason, decided_by, decided_at, created_at, updated_at, version)
VALUES
    ((SELECT id FROM users WHERE email = 'maria.lopez@test.com'), 25000000.00, 36, 22.0000, NULL, NULL, NULL, NULL, NULL,
     'PENDING', NULL, NULL, NULL, now() - interval '1 days' - interval '22 hours', now() - interval '1 days' - interval '22 hours', 0),
    ((SELECT id FROM users WHERE email = 'maria.lopez@test.com'), 8000000.00, 12, 18.0000, 17.5000, 1.352972, 726739.34, 720872.06, 8720872.06,
     'APPROVED', NULL, (SELECT id FROM users WHERE email = 'admin@test.com'), now() - interval '19 days', now() - interval '20 days' - interval '15 hours', now() - interval '20 days' - interval '15 hours', 1),
    ((SELECT id FROM users WHERE email = 'carlos.ruiz@test.com'), 50000000.00, 60, 25.0000, NULL, NULL, NULL, NULL, NULL,
     'PENDING', NULL, NULL, NULL, now() - interval '2 days' - interval '6 hours', now() - interval '2 days' - interval '6 hours', 0),
    ((SELECT id FROM users WHERE email = 'carlos.ruiz@test.com'), 120000000.00, 84, 25.0000, NULL, NULL, NULL, NULL, NULL,
     'REJECTED', 'El nivel de endeudamiento actual supera el máximo permitido para el monto solicitado.', (SELECT id FROM users WHERE email = 'sofia.ramirez@test.com'), now() - interval '11 days', now() - interval '12 days' - interval '13 hours', now() - interval '12 days' - interval '13 hours', 1),
    ((SELECT id FROM users WHERE email = 'ana.torres@test.com'), 15000000.00, 24, 22.0000, 21.0000, 1.601187, 757691.52, 3184596.46, 18184596.46,
     'APPROVED', NULL, (SELECT id FROM users WHERE email = 'sofia.ramirez@test.com'), now() - interval '29 days', now() - interval '30 days' - interval '7 hours', now() - interval '30 days' - interval '7 hours', 1),
    ((SELECT id FROM users WHERE email = 'ana.torres@test.com'), 3000000.00, 6, 18.0000, NULL, NULL, NULL, NULL, NULL,
     'PENDING', NULL, NULL, NULL, now() - interval '0 days' - interval '19 hours', now() - interval '0 days' - interval '19 hours', 0),
    ((SELECT id FROM users WHERE email = 'ana.torres@test.com'), 5000000.00, 12, 18.0000, NULL, NULL, NULL, NULL, NULL,
     'CANCELLED', NULL, NULL, NULL, now() - interval '15 days' - interval '15 hours', now() - interval '15 days' - interval '15 hours', 1),
    ((SELECT id FROM users WHERE email = 'jorge.medina@test.com'), 80000000.00, 48, 25.0000, NULL, NULL, NULL, NULL, NULL,
     'PENDING', NULL, NULL, NULL, now() - interval '3 days' - interval '14 hours', now() - interval '3 days' - interval '14 hours', 0),
    ((SELECT id FROM users WHERE email = 'jorge.medina@test.com'), 30000000.00, 36, 22.0000, 23.0000, 1.740084, 1128420.03, 10623121.13, 40623121.13,
     'APPROVED', NULL, (SELECT id FROM users WHERE email = 'admin@test.com'), now() - interval '44 days', now() - interval '45 days' - interval '22 hours', now() - interval '45 days' - interval '22 hours', 1),
    ((SELECT id FROM users WHERE email = 'pedro.gomez@test.com'), 2000000.00, 12, 18.0000, NULL, NULL, NULL, NULL, NULL,
     'REJECTED', 'Historial crediticio con reportes negativos en centrales de riesgo.', (SELECT id FROM users WHERE email = 'admin@test.com'), now() - interval '59 days', now() - interval '60 days' - interval '15 hours', now() - interval '60 days' - interval '15 hours', 1);

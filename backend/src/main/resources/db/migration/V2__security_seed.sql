-- Datos semilla de seguridad. Password de ambos usuarios: 123 (BCrypt)

INSERT INTO roles (code, name) VALUES
    ('USER',  'Usuario'),
    ('ADMIN', 'Administrador');

INSERT INTO permissions (code, description) VALUES
    ('CREDIT_REQUEST',    'Solicitar créditos'),
    ('CREDIT_VIEW_OWN',   'Consultar sus propios créditos'),
    ('CREDIT_CANCEL_OWN', 'Cancelar sus solicitudes pendientes'),
    ('CREDIT_SIMULATE',   'Simular cuota de un crédito'),
    ('CREDIT_VIEW_ALL',   'Consultar todos los créditos'),
    ('CREDIT_APPROVE',    'Aprobar créditos'),
    ('CREDIT_REJECT',     'Rechazar créditos'),
    ('USER_MANAGE',       'Administrar usuarios'),
    ('RATE_MANAGE',       'Administrar tramos de tasa de interés');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('CREDIT_REQUEST', 'CREDIT_VIEW_OWN', 'CREDIT_CANCEL_OWN', 'CREDIT_SIMULATE')
WHERE r.code = 'USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('CREDIT_SIMULATE', 'CREDIT_VIEW_ALL', 'CREDIT_APPROVE', 'CREDIT_REJECT',
                                 'USER_MANAGE', 'RATE_MANAGE')
WHERE r.code = 'ADMIN';

INSERT INTO users (full_name, email, password, role_id)
SELECT 'Usuario Demo', 'usuario@test.com', '$2a$10$JUv1GoccusdSelHqWKM79OO.Kq11FVQl.LSoSNODTKxaAJIPEMeWe', id
FROM roles WHERE code = 'USER';

INSERT INTO users (full_name, email, password, role_id)
SELECT 'Administrador', 'admin@test.com', '$2a$10$JUv1GoccusdSelHqWKM79OO.Kq11FVQl.LSoSNODTKxaAJIPEMeWe', id
FROM roles WHERE code = 'ADMIN';

-- Esquema de seguridad: roles, permisos y usuarios

CREATE TABLE roles (
    id   BIGSERIAL    PRIMARY KEY,
    code VARCHAR(30)  NOT NULL UNIQUE,
    name VARCHAR(80)  NOT NULL
);

CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(150) NOT NULL
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    role_id    BIGINT       NOT NULL REFERENCES roles (id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email ON users (lower(email));
CREATE INDEX ix_users_role ON users (role_id);

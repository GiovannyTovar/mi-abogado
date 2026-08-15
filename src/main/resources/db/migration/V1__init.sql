-- =====================================================================
-- V1 - Baseline: tenancy, usuarios/auth y dominio lawyer.
-- Solo se crean las tablas que las Fases 0 y 1 necesitan. Las tablas de
-- casos, documentos, facturacion, etc. llegan en su propia migracion
-- cuando se implemente su fase (no se crean tablas para codigo que no existe).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()

-- ---------------------------------------------------------------------
-- tenant: la firma / bufete. Raiz del aislamiento multi-tenant.
-- ---------------------------------------------------------------------
CREATE TABLE tenant (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(150) NOT NULL,
    slug          VARCHAR(80)  NOT NULL,
    nit           VARCHAR(30),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(30),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_slug   UNIQUE (slug),
    CONSTRAINT ck_tenant_status CHECK (status IN ('ACTIVE', 'TRIAL', 'SUSPENDED', 'CANCELLED'))
);

COMMENT ON COLUMN tenant.slug IS 'Identificador URL-safe de la firma; se usara como subdominio/ruta del portal white-label.';

-- ---------------------------------------------------------------------
-- app_user: identidad unica de la plataforma ("user" es palabra reservada).
-- Un usuario de Firebase = una fila aqui. El rol y el tenant viven aqui,
-- no en Firebase, para que la autorizacion sea auditable en nuestra BD.
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         REFERENCES tenant (id) ON DELETE RESTRICT,
    firebase_uid  VARCHAR(128),
    email         VARCHAR(180) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    photo_url     VARCHAR(500),
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_firebase_uid UNIQUE (firebase_uid),
    CONSTRAINT ck_app_user_role   CHECK (role IN ('SUPER_ADMIN', 'FIRM_OWNER', 'LAWYER', 'ASSISTANT', 'CLIENT')),
    CONSTRAINT ck_app_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    -- El super-admin es de plataforma: no pertenece a ninguna firma.
    -- Todo el resto obligatoriamente si.
    CONSTRAINT ck_app_user_tenant CHECK (
        (role = 'SUPER_ADMIN' AND tenant_id IS NULL)
        OR (role <> 'SUPER_ADMIN' AND tenant_id IS NOT NULL)
    )
);

-- Email unico dentro de la firma; y unico global entre super-admins.
CREATE UNIQUE INDEX uq_app_user_email_per_tenant
    ON app_user (tenant_id, lower(email)) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX uq_app_user_email_platform
    ON app_user (lower(email)) WHERE tenant_id IS NULL;
CREATE INDEX ix_app_user_tenant ON app_user (tenant_id);

COMMENT ON COLUMN app_user.firebase_uid IS 'NULL mientras el usuario esta invitado (PENDING); se vincula en su primer login con Google.';

-- ---------------------------------------------------------------------
-- refresh_token: rotacion y revocacion de sesiones. Se guarda solo el
-- hash SHA-256, nunca el token en claro.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_token (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_user ON refresh_token (user_id) WHERE revoked_at IS NULL;

-- ---------------------------------------------------------------------
-- practice_area: catalogo global de especialidades laborales.
-- NO es multi-tenant: lo mantiene la plataforma y lo comparten todas las firmas.
-- ---------------------------------------------------------------------
CREATE TABLE practice_area (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(60)  NOT NULL,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_practice_area_code UNIQUE (code)
);

-- ---------------------------------------------------------------------
-- lawyer: perfil profesional del abogado. Extiende a app_user (1:1),
-- no lo duplica. Tenant-scoped.
-- ---------------------------------------------------------------------
CREATE TABLE lawyer (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    user_id             UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    license_number      VARCHAR(40)  NOT NULL,
    bio                 TEXT,
    years_of_experience SMALLINT     NOT NULL DEFAULT 0,
    city                VARCHAR(100),
    public_slug         VARCHAR(140),
    published           BOOLEAN      NOT NULL DEFAULT FALSE,
    hourly_rate         NUMERIC(12, 2),
    rating_avg          NUMERIC(3, 2) NOT NULL DEFAULT 0,
    rating_count        INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_lawyer_user           UNIQUE (user_id),
    CONSTRAINT uq_lawyer_license        UNIQUE (tenant_id, license_number),
    CONSTRAINT uq_lawyer_public_slug    UNIQUE (public_slug),
    CONSTRAINT ck_lawyer_experience     CHECK (years_of_experience >= 0),
    CONSTRAINT ck_lawyer_rating         CHECK (rating_avg BETWEEN 0 AND 5 AND rating_count >= 0),
    CONSTRAINT ck_lawyer_hourly_rate    CHECK (hourly_rate IS NULL OR hourly_rate >= 0),
    -- Para aparecer en el directorio publico (Fase 8) hacen falta ciudad y slug.
    CONSTRAINT ck_lawyer_publishable    CHECK (
        NOT published OR (city IS NOT NULL AND public_slug IS NOT NULL)
    )
);

CREATE INDEX ix_lawyer_tenant    ON lawyer (tenant_id);
CREATE INDEX ix_lawyer_directory ON lawyer (city) WHERE published;

COMMENT ON COLUMN lawyer.license_number IS 'Tarjeta profesional expedida por el Consejo Superior de la Judicatura.';

-- ---------------------------------------------------------------------
-- lawyer_practice_area: especialidades del abogado (N:M con el catalogo).
-- ---------------------------------------------------------------------
CREATE TABLE lawyer_practice_area (
    lawyer_id        UUID NOT NULL REFERENCES lawyer (id) ON DELETE CASCADE,
    practice_area_id UUID NOT NULL REFERENCES practice_area (id) ON DELETE RESTRICT,
    PRIMARY KEY (lawyer_id, practice_area_id)
);

CREATE INDEX ix_lawyer_practice_area_area ON lawyer_practice_area (practice_area_id);

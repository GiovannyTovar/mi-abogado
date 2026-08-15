-- =====================================================================
-- V3 - Fase 2: planes y suscripciones de las firmas a la plataforma.
-- Ojo: esto es lo que la firma le paga a Mi Abogado. Lo que la firma le
-- cobra a sus clientes (honorarios) es otra cosa y llega en la Fase 6.
-- =====================================================================

-- ---------------------------------------------------------------------
-- subscription_plan: catalogo de planes. Global, no multi-tenant.
-- Los limites nulos significan "sin limite".
-- ---------------------------------------------------------------------
CREATE TABLE subscription_plan (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(40)   NOT NULL,
    name                VARCHAR(120)  NOT NULL,
    description         VARCHAR(400),
    monthly_price       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'COP',
    trial_days          SMALLINT      NOT NULL DEFAULT 0,
    max_members         INTEGER,
    max_active_cases    INTEGER,
    marketplace_enabled BOOLEAN       NOT NULL DEFAULT FALSE,
    white_label_enabled BOOLEAN       NOT NULL DEFAULT FALSE,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order          SMALLINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscription_plan_code  UNIQUE (code),
    CONSTRAINT ck_subscription_plan_price CHECK (monthly_price >= 0),
    CONSTRAINT ck_subscription_plan_trial CHECK (trial_days >= 0),
    CONSTRAINT ck_subscription_plan_limits CHECK (
        (max_members IS NULL OR max_members > 0)
        AND (max_active_cases IS NULL OR max_active_cases > 0)
    )
);

COMMENT ON COLUMN subscription_plan.max_members IS 'Miembros activos de la firma (abogados + asistentes + dueno). NULL = ilimitado.';

-- ---------------------------------------------------------------------
-- subscription: plan vigente de una firma.
-- No lleva @TenantId: el super-admin la consulta entre firmas y el login
-- la necesita antes de resolver el tenant. Ver docs/ARQUITECTURA.md 4.2.
-- ---------------------------------------------------------------------
CREATE TABLE subscription (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    plan_id            UUID        NOT NULL REFERENCES subscription_plan (id) ON DELETE RESTRICT,
    status             VARCHAR(20) NOT NULL,
    started_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    trial_ends_at      TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancelled_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_subscription_status CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED')),
    CONSTRAINT ck_subscription_trial  CHECK (status <> 'TRIALING' OR trial_ends_at IS NOT NULL)
);

-- Una sola suscripcion vigente por firma; las canceladas quedan como historico.
CREATE UNIQUE INDEX uq_subscription_current_per_tenant
    ON subscription (tenant_id) WHERE status <> 'CANCELLED';
CREATE INDEX ix_subscription_plan ON subscription (plan_id);

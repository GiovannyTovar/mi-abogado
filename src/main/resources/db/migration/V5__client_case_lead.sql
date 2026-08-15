-- =====================================================================
-- V5 - Fase 3: CRM de clientes, expedientes, bitacora, terminos y leads.
-- Todas las tablas de esta migracion son del plano de firma: llevan
-- tenant_id y sus entidades heredan de TenantScopedEntity.
-- =====================================================================

-- ---------------------------------------------------------------------
-- client: el cliente de la firma (persona natural o juridica).
-- Todavia no se enlaza con app_user: eso llega en la Fase 4, cuando el
-- cliente pueda entrar al portal.
-- ---------------------------------------------------------------------
CREATE TABLE client (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    client_type     VARCHAR(20)  NOT NULL,
    document_type   VARCHAR(20)  NOT NULL,
    document_number VARCHAR(30)  NOT NULL,
    name            VARCHAR(180) NOT NULL,
    email           VARCHAR(180),
    phone           VARCHAR(30),
    address         VARCHAR(250),
    city            VARCHAR(100),
    notes           TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_client_document   UNIQUE (tenant_id, document_type, document_number),
    CONSTRAINT ck_client_type       CHECK (client_type IN ('NATURAL', 'JURIDICA')),
    CONSTRAINT ck_client_doc_type   CHECK (document_type IN ('CC', 'CE', 'NIT', 'PASAPORTE', 'PEP')),
    CONSTRAINT ck_client_status     CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_client_tenant ON client (tenant_id);
CREATE INDEX ix_client_name   ON client (tenant_id, lower(name));

COMMENT ON COLUMN client.name IS 'Nombre completo si es persona natural; razon social si es juridica.';

-- ---------------------------------------------------------------------
-- legal_case: el expediente. "case" es palabra reservada en SQL y en Java,
-- de ahi el prefijo.
-- ---------------------------------------------------------------------
CREATE TABLE legal_case (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID          NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    client_id          UUID          NOT NULL REFERENCES client (id) ON DELETE RESTRICT,
    assigned_lawyer_id UUID          REFERENCES lawyer (id) ON DELETE SET NULL,
    practice_area_id   UUID          REFERENCES practice_area (id) ON DELETE SET NULL,
    created_by         UUID          REFERENCES app_user (id) ON DELETE SET NULL,
    case_number        VARCHAR(20)   NOT NULL,
    radicado           VARCHAR(30),
    title              VARCHAR(200)  NOT NULL,
    description        TEXT,
    case_type          VARCHAR(20)   NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    outcome            VARCHAR(20),
    priority           VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    court              VARCHAR(180),
    opposing_party     VARCHAR(180),
    claim_amount       NUMERIC(14, 2),
    opened_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    closed_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_case_number      UNIQUE (tenant_id, case_number),
    CONSTRAINT ck_case_type        CHECK (case_type IN ('LITIGIO', 'ASESORIA')),
    CONSTRAINT ck_case_status      CHECK (status IN ('OPEN', 'IN_PROGRESS', 'ON_HOLD', 'CLOSED')),
    CONSTRAINT ck_case_outcome     CHECK (outcome IS NULL OR outcome IN ('WON', 'LOST', 'SETTLED', 'WITHDRAWN')),
    CONSTRAINT ck_case_priority    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_case_amount      CHECK (claim_amount IS NULL OR claim_amount >= 0),
    -- Un caso cerrado tiene desenlace y fecha de cierre; uno abierto no tiene ninguno.
    CONSTRAINT ck_case_closed      CHECK (
        (status = 'CLOSED' AND outcome IS NOT NULL AND closed_at IS NOT NULL)
        OR (status <> 'CLOSED' AND outcome IS NULL AND closed_at IS NULL)
    )
);

-- El radicado del juzgado es unico dentro de la firma cuando existe (solo litigio).
CREATE UNIQUE INDEX uq_case_radicado ON legal_case (tenant_id, radicado) WHERE radicado IS NOT NULL;
CREATE INDEX ix_case_tenant_status ON legal_case (tenant_id, status);
CREATE INDEX ix_case_client        ON legal_case (client_id);
CREATE INDEX ix_case_lawyer        ON legal_case (assigned_lawyer_id);

COMMENT ON COLUMN legal_case.case_number IS 'Consecutivo interno de la firma, formato AAAA-NNNN.';
COMMENT ON COLUMN legal_case.radicado    IS 'Numero de radicacion del juzgado (23 digitos en la Rama Judicial).';

-- ---------------------------------------------------------------------
-- case_number_sequence: consecutivo por firma y ano.
-- Tabla propia en vez de "max(case_number) + 1": ese calculo se pisa
-- cuando dos personas crean un caso a la vez.
-- ---------------------------------------------------------------------
CREATE TABLE case_number_sequence (
    tenant_id   UUID     NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    year        SMALLINT NOT NULL,
    last_number INTEGER  NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, year)
);

-- ---------------------------------------------------------------------
-- case_event: bitacora del expediente. Solo se agrega, no se edita:
-- es el historial de lo que paso.
-- ---------------------------------------------------------------------
CREATE TABLE case_event (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    legal_case_id UUID         NOT NULL REFERENCES legal_case (id) ON DELETE CASCADE,
    created_by    UUID         REFERENCES app_user (id) ON DELETE SET NULL,
    event_type    VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_case_event_type CHECK (
        event_type IN ('NOTA', 'ACTUACION', 'AUDIENCIA', 'DOCUMENTO', 'CAMBIO_ESTADO', 'COMUNICACION')
    )
);

CREATE INDEX ix_case_event_case ON case_event (legal_case_id, occurred_at DESC);

-- ---------------------------------------------------------------------
-- case_deadline: terminos procesales y vencimientos. El corazon de las
-- alertas: en materia laboral un termino perdido es un caso perdido.
-- ---------------------------------------------------------------------
CREATE TABLE case_deadline (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    legal_case_id      UUID         NOT NULL REFERENCES legal_case (id) ON DELETE CASCADE,
    created_by         UUID         REFERENCES app_user (id) ON DELETE SET NULL,
    completed_by       UUID         REFERENCES app_user (id) ON DELETE SET NULL,
    deadline_type      VARCHAR(20)  NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    due_date           DATE         NOT NULL,
    notify_days_before SMALLINT     NOT NULL DEFAULT 3,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_deadline_type   CHECK (deadline_type IN ('TERMINO_LEGAL', 'AUDIENCIA', 'VENCIMIENTO', 'RECORDATORIO')),
    CONSTRAINT ck_deadline_status CHECK (status IN ('PENDING', 'COMPLETED', 'MISSED')),
    CONSTRAINT ck_deadline_notify CHECK (notify_days_before >= 0),
    CONSTRAINT ck_deadline_completed CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    )
);

-- Indice de la consulta caliente: "que vence en los proximos N dias".
CREATE INDEX ix_deadline_agenda ON case_deadline (tenant_id, due_date) WHERE status = 'PENDING';
CREATE INDEX ix_deadline_case   ON case_deadline (legal_case_id);

-- ---------------------------------------------------------------------
-- lead: contacto que aun no es cliente. Alimenta el pipeline de captacion;
-- el marketplace (Fase 8) y la calculadora publica (Fase 5) crearan leads.
-- ---------------------------------------------------------------------
CREATE TABLE lead (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    practice_area_id    UUID         REFERENCES practice_area (id) ON DELETE SET NULL,
    assigned_lawyer_id  UUID         REFERENCES lawyer (id) ON DELETE SET NULL,
    converted_client_id UUID         REFERENCES client (id) ON DELETE SET NULL,
    name                VARCHAR(180) NOT NULL,
    email               VARCHAR(180),
    phone               VARCHAR(30),
    city                VARCHAR(100),
    source              VARCHAR(20)  NOT NULL,
    message             TEXT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    lost_reason         VARCHAR(250),
    contacted_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_source CHECK (source IN ('MARKETPLACE', 'CALCULADORA', 'REFERIDO', 'WEB', 'TELEFONO', 'OTRO')),
    CONSTRAINT ck_lead_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CONVERTED', 'LOST')),
    -- Un lead convertido apunta al cliente que se creo; uno no convertido, no.
    CONSTRAINT ck_lead_converted CHECK (
        (status = 'CONVERTED' AND converted_client_id IS NOT NULL)
        OR (status <> 'CONVERTED' AND converted_client_id IS NULL)
    ),
    -- Sin telefono ni correo no hay a quien contactar.
    CONSTRAINT ck_lead_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE INDEX ix_lead_tenant_status ON lead (tenant_id, status);

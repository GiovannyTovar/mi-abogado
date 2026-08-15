-- =====================================================================
-- V6 - Fase 4: portal del cliente.
-- Documentos, mensajeria por expediente, agenda de citas y el enlace
-- entre la ficha del CRM y el usuario que entra al portal.
-- =====================================================================

-- ---------------------------------------------------------------------
-- El cliente ya puede tener acceso. Se invita como cualquier otro usuario
-- (rol CLIENT, estado PENDING) y se vincula en su primer login con Google.
-- ---------------------------------------------------------------------
ALTER TABLE client
    ADD COLUMN user_id UUID REFERENCES app_user (id) ON DELETE SET NULL;

-- Un usuario del portal representa a un solo cliente.
CREATE UNIQUE INDEX uq_client_user ON client (user_id) WHERE user_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- No todo lo que se anota en la bitacora es para el cliente: las notas de
-- estrategia son internas. Por defecto nada se comparte; la firma decide
-- que publicar en el portal.
-- ---------------------------------------------------------------------
ALTER TABLE case_event
    ADD COLUMN visible_to_client BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_case_event_shared ON case_event (legal_case_id, occurred_at DESC)
    WHERE visible_to_client;

-- ---------------------------------------------------------------------
-- document: archivo del expediente. La BD guarda metadatos; el binario
-- vive en disco (volumen del VPS) bajo storage_key.
--
-- La tabla nace preparada para la IA de v2 sin traer nada de IA hoy:
-- extracted_text queda vacio y lo llenara el modulo document cuando se
-- conecte OCR o un LLM. Ninguna otra parte del sistema lee esa columna.
-- ---------------------------------------------------------------------
CREATE TABLE document (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    legal_case_id  UUID         NOT NULL REFERENCES legal_case (id) ON DELETE CASCADE,
    uploaded_by    UUID         REFERENCES app_user (id) ON DELETE SET NULL,
    name           VARCHAR(255) NOT NULL,
    storage_key    VARCHAR(400) NOT NULL,
    content_type   VARCHAR(120) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    visibility     VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL',
    source         VARCHAR(20)  NOT NULL DEFAULT 'FIRM',
    description    VARCHAR(400),
    extracted_text TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_document_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_document_visibility  CHECK (visibility IN ('INTERNAL', 'SHARED_WITH_CLIENT')),
    CONSTRAINT ck_document_source      CHECK (source IN ('FIRM', 'CLIENT')),
    CONSTRAINT ck_document_size        CHECK (size_bytes > 0)
);

CREATE INDEX ix_document_case   ON document (legal_case_id, created_at DESC);
CREATE INDEX ix_document_shared ON document (legal_case_id) WHERE visibility = 'SHARED_WITH_CLIENT';

COMMENT ON COLUMN document.storage_key    IS 'Ruta relativa dentro del almacen: {tenant}/{caso}/{uuid}.{ext}. No contiene el nombre original.';
COMMENT ON COLUMN document.extracted_text IS 'Reservado para v2 (OCR / LLM). Hoy siempre NULL.';

-- ---------------------------------------------------------------------
-- case_message: hilo de mensajes entre la firma y su cliente, por expediente.
-- Un solo hilo por caso: no hace falta una tabla de conversaciones.
-- ---------------------------------------------------------------------
CREATE TABLE case_message (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    legal_case_id UUID        NOT NULL REFERENCES legal_case (id) ON DELETE CASCADE,
    sender_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    body          TEXT        NOT NULL,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_case_message_thread ON case_message (legal_case_id, created_at DESC);
-- Contador de no leidos, que se consulta en cada carga del portal.
CREATE INDEX ix_case_message_unread ON case_message (legal_case_id) WHERE read_at IS NULL;

-- ---------------------------------------------------------------------
-- appointment: citas con el cliente. Puede o no colgar de un expediente
-- (la primera cita suele ser antes de que exista el caso).
-- ---------------------------------------------------------------------
CREATE TABLE appointment (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    client_id     UUID         NOT NULL REFERENCES client (id) ON DELETE CASCADE,
    legal_case_id UUID         REFERENCES legal_case (id) ON DELETE SET NULL,
    lawyer_id     UUID         REFERENCES lawyer (id) ON DELETE SET NULL,
    created_by    UUID         REFERENCES app_user (id) ON DELETE SET NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    mode          VARCHAR(20)  NOT NULL,
    location      VARCHAR(250),
    starts_at     TIMESTAMPTZ  NOT NULL,
    ends_at       TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    cancel_reason VARCHAR(250),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_appointment_mode   CHECK (mode IN ('PRESENCIAL', 'VIRTUAL', 'TELEFONICA')),
    CONSTRAINT ck_appointment_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_appointment_range  CHECK (ends_at > starts_at)
);

CREATE INDEX ix_appointment_agenda ON appointment (tenant_id, starts_at);
CREATE INDEX ix_appointment_lawyer ON appointment (lawyer_id, starts_at);
CREATE INDEX ix_appointment_client ON appointment (client_id, starts_at);

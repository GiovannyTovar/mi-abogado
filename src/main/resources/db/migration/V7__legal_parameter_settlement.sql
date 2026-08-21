-- =====================================================================
-- V7 - Fase 5: parametros legales del ano y calculadora de liquidacion.
--
-- legal_parameter es catalogo de plataforma (como practice_area): NO lleva
-- tenant_id. El salario minimo no cambia de una firma a otra, y la
-- calculadora publica lo consulta sin sesion, cuando todavia no hay tenant.
--
-- settlement_calculation si es del plano de firma: es trabajo del despacho
-- sobre un cliente y, casi siempre, sobre un expediente.
-- =====================================================================

-- ---------------------------------------------------------------------
-- legal_parameter: las cifras que el Gobierno fija cada ano y de las que
-- cuelga toda liquidacion laboral. Se guardan en tabla, no en constantes
-- de codigo, por dos razones:
--   1. en diciembre cambian y no puede hacer falta un despliegue;
--   2. una liquidacion de 2022 debe seguir calculandose con el SMLMV de
--      2022, asi que hacen falta todos los anos, no solo el vigente.
-- ---------------------------------------------------------------------
CREATE TABLE legal_parameter (
    id                           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    year                         INT           NOT NULL,
    minimum_wage                 NUMERIC(14, 2) NOT NULL,
    transport_allowance          NUMERIC(14, 2) NOT NULL,
    uvt                          NUMERIC(14, 2) NOT NULL,
    severance_interest_rate      NUMERIC(6, 4)  NOT NULL DEFAULT 0.1200,
    transport_allowance_wage_cap NUMERIC(5, 2)  NOT NULL DEFAULT 2.00,
    high_salary_threshold        NUMERIC(5, 2)  NOT NULL DEFAULT 10.00,
    created_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_legal_parameter_year   UNIQUE (year),
    CONSTRAINT ck_legal_parameter_year   CHECK (year BETWEEN 1990 AND 2100),
    CONSTRAINT ck_legal_parameter_wage   CHECK (minimum_wage > 0)
);

COMMENT ON COLUMN legal_parameter.transport_allowance_wage_cap IS
    'Tope, en SMLMV, hasta el que se causa auxilio de transporte. Hoy 2.';
COMMENT ON COLUMN legal_parameter.high_salary_threshold IS
    'Umbral, en SMLMV, a partir del cual la indemnizacion del art. 64 CST baja a 20+15 dias. Hoy 10.';
COMMENT ON COLUMN legal_parameter.severance_interest_rate IS
    'Interes anual sobre cesantias (art. 99 Ley 50/1990). Hoy 12%.';

INSERT INTO legal_parameter (year, minimum_wage, transport_allowance, uvt) VALUES
    (2020,  877803.00, 102854.00, 35607.00),
    (2021,  908526.00, 106454.00, 36308.00),
    (2022, 1000000.00, 117172.00, 38004.00),
    (2023, 1160000.00, 140606.00, 42412.00),
    (2024, 1300000.00, 162000.00, 47065.00),
    (2025, 1423500.00, 200000.00, 49799.00);

-- ---------------------------------------------------------------------
-- settlement_calculation: una liquidacion guardada.
--
-- Guarda la entrada Y el resultado. No se recalcula al leer: si manana se
-- corrige una formula o se carga otro parametro, la liquidacion que la
-- firma ya le entrego al cliente tiene que seguir diciendo lo mismo.
-- Por eso tampoco hay PATCH: se calcula otra vez y se guarda de nuevo.
-- ---------------------------------------------------------------------
CREATE TABLE settlement_calculation (
    id                         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                  UUID          NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    client_id                  UUID          REFERENCES client (id) ON DELETE SET NULL,
    legal_case_id              UUID          REFERENCES legal_case (id) ON DELETE CASCADE,
    created_by                 UUID          REFERENCES app_user (id) ON DELETE SET NULL,

    -- entrada
    employee_name              VARCHAR(180)  NOT NULL,
    contract_type              VARCHAR(20)   NOT NULL,
    termination_reason         VARCHAR(30)   NOT NULL,
    monthly_salary             NUMERIC(14, 2) NOT NULL,
    variable_average           NUMERIC(14, 2) NOT NULL DEFAULT 0,
    start_date                 DATE          NOT NULL,
    end_date                   DATE          NOT NULL,
    contract_end_date          DATE,
    transport_allowance_applies BOOLEAN      NOT NULL DEFAULT TRUE,
    severance_paid_through     DATE,
    service_bonus_paid_through DATE,
    vacation_days_taken        NUMERIC(6, 2) NOT NULL DEFAULT 0,
    notes                      TEXT,

    -- resultado congelado
    parameter_year             INT           NOT NULL,
    minimum_wage               NUMERIC(14, 2) NOT NULL,
    transport_allowance        NUMERIC(14, 2) NOT NULL,
    days_worked                INT           NOT NULL,
    severance_days             INT           NOT NULL,
    severance                  NUMERIC(14, 2) NOT NULL,
    severance_interest         NUMERIC(14, 2) NOT NULL,
    service_bonus_days         INT           NOT NULL,
    service_bonus              NUMERIC(14, 2) NOT NULL,
    vacation_days              NUMERIC(8, 2) NOT NULL,
    vacation                   NUMERIC(14, 2) NOT NULL,
    indemnity_days             NUMERIC(8, 2) NOT NULL,
    indemnity                  NUMERIC(14, 2) NOT NULL,
    total                      NUMERIC(14, 2) NOT NULL,

    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_settlement_contract CHECK (contract_type IN ('INDEFINIDO', 'FIJO', 'OBRA_LABOR')),
    CONSTRAINT ck_settlement_reason   CHECK (termination_reason IN (
        'SIN_JUSTA_CAUSA', 'JUSTA_CAUSA', 'RENUNCIA', 'MUTUO_ACUERDO', 'VENCIMIENTO_PLAZO', 'OBRA_TERMINADA')),
    CONSTRAINT ck_settlement_dates    CHECK (end_date >= start_date),
    CONSTRAINT ck_settlement_salary   CHECK (monthly_salary > 0)
);

CREATE INDEX ix_settlement_tenant ON settlement_calculation (tenant_id, created_at DESC);
CREATE INDEX ix_settlement_client ON settlement_calculation (client_id) WHERE client_id IS NOT NULL;
CREATE INDEX ix_settlement_case   ON settlement_calculation (legal_case_id) WHERE legal_case_id IS NOT NULL;

COMMENT ON COLUMN settlement_calculation.variable_average IS
    'Promedio mensual de lo variable (comisiones, recargos, horas extra): entra a la base de prestaciones.';
COMMENT ON COLUMN settlement_calculation.severance_paid_through IS
    'Fecha hasta la que ya se consignaron cesantias al fondo. Null = se deben desde el ingreso.';

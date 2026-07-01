-- ============================================================
-- V3 — Calculadora de Ahorro (moneda extranjera)
-- ============================================================

ALTER TABLE calculadoras
    DROP CONSTRAINT IF EXISTS calculadoras_tipo_check;

ALTER TABLE calculadoras
    ADD CONSTRAINT calculadoras_tipo_check
        CHECK (tipo IN ('GENERICA', 'SUBE', 'AHORRO'));

CREATE TABLE calculadora_ahorro (
    id                    BIGSERIAL PRIMARY KEY,
    calculadora_id        BIGINT NOT NULL UNIQUE REFERENCES calculadoras(id) ON DELETE CASCADE,
    objetivo_usd          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cotizacion_congelada  NUMERIC(15, 4),
    fecha_congelamiento   TIMESTAMP,
    comprado              BOOLEAN NOT NULL DEFAULT false
);

-- ============================================================
-- V4 — Historial de operaciones de ahorro + categorías extra
-- ============================================================

-- Nuevas categorías
INSERT INTO categorias (nombre, icono) VALUES
    ('Ahorro / Inversiones', '💰'),
    ('Servicios',            '🧾'),
    ('Viajes',               '✈️');

-- Reestructurar calculadora_ahorro: solo guarda el objetivo actual (draft)
-- Eliminar campos de "compra congelada" que ahora van al historial
ALTER TABLE calculadora_ahorro
    DROP COLUMN IF EXISTS cotizacion_congelada,
    DROP COLUMN IF EXISTS fecha_congelamiento,
    DROP COLUMN IF EXISTS comprado;

-- Historial de operaciones efectivizadas (1:N con calculadora_ahorro)
CREATE TABLE ahorro_operaciones (
    id                BIGSERIAL PRIMARY KEY,
    ahorro_id         BIGINT NOT NULL REFERENCES calculadora_ahorro(id) ON DELETE CASCADE,
    objetivo_usd      NUMERIC(12, 2) NOT NULL,
    cotizacion        NUMERIC(15, 4) NOT NULL,
    monto_ars         NUMERIC(15, 2) NOT NULL,
    concepto          VARCHAR(200) NOT NULL,
    fecha             DATE NOT NULL DEFAULT CURRENT_DATE,
    mes_impacto       INTEGER NOT NULL,
    anio_impacto      INTEGER NOT NULL
);

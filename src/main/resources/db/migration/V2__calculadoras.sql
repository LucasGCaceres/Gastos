-- ============================================================
-- V2 — Calculadoras paramétricas de gastos
-- ============================================================

CREATE TABLE calculadoras (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    tipo         VARCHAR(20)  NOT NULL CHECK (tipo IN ('GENERICA', 'SUBE')),
    gasto_fijo_id BIGINT REFERENCES gastos_fijos(id),
    activa       BOOLEAN NOT NULL DEFAULT true
);

-- Ítems para calculadoras genéricas (cantidad × precio_unitario)
CREATE TABLE calculadora_items (
    id               BIGSERIAL PRIMARY KEY,
    calculadora_id   BIGINT NOT NULL REFERENCES calculadoras(id) ON DELETE CASCADE,
    nombre           VARCHAR(100) NOT NULL,
    cantidad         NUMERIC(10, 2) NOT NULL,
    precio_unitario  NUMERIC(15, 2) NOT NULL,
    orden            INTEGER NOT NULL DEFAULT 0
);

-- Rutas para calculadoras SUBE (ej: "Facultad ida/vuelta")
CREATE TABLE calculadora_rutas (
    id               BIGSERIAL PRIMARY KEY,
    calculadora_id   BIGINT NOT NULL REFERENCES calculadoras(id) ON DELETE CASCADE,
    nombre           VARCHAR(100) NOT NULL,
    viajes_por_mes   INTEGER NOT NULL DEFAULT 20
);

-- Tramos de cada ruta (orden define el descuento: 0=100%, 1=50%, 2+=25%)
CREATE TABLE calculadora_tramos (
    id        BIGSERIAL PRIMARY KEY,
    ruta_id   BIGINT NOT NULL REFERENCES calculadora_rutas(id) ON DELETE CASCADE,
    nombre    VARCHAR(50) NOT NULL,
    precio    NUMERIC(10, 2) NOT NULL,
    orden     INTEGER NOT NULL DEFAULT 0
);

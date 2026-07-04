-- ============================================================
-- V6 — Eventos de gasto (salidas / ocasiones con varios ítems)
-- ============================================================

-- Cabecera de un evento puntual (ej: "Salida fiesta"), atado a un ciclo
CREATE TABLE eventos_gasto (
    id               BIGSERIAL PRIMARY KEY,
    ciclo_mensual_id BIGINT       NOT NULL REFERENCES ciclos_mensuales(id),
    nombre           VARCHAR(150) NOT NULL,
    fecha            DATE         NOT NULL DEFAULT CURRENT_DATE
);

-- Ítems de gasto dentro del evento (ej: Uber, Cerveza, Medialunas)
CREATE TABLE eventos_gasto_items (
    id        BIGSERIAL PRIMARY KEY,
    evento_id BIGINT         NOT NULL REFERENCES eventos_gasto(id) ON DELETE CASCADE,
    concepto  VARCHAR(255)   NOT NULL,
    monto     NUMERIC(15, 2) NOT NULL,
    orden     INTEGER        NOT NULL DEFAULT 0
);

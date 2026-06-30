-- ============================================================
-- V1 — Esquema inicial del sistema de finanzas personales
-- ============================================================

-- Ciclos mensuales (un registro por mes)
CREATE TABLE ciclos_mensuales (
    id              BIGSERIAL PRIMARY KEY,
    anio            INTEGER        NOT NULL,
    mes             INTEGER        NOT NULL CHECK (mes BETWEEN 1 AND 12),
    total_ingresos  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_fijos     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_variables NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_cuotas    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    saldo_final     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    estado          VARCHAR(10)    NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO')),
    fecha_cierre_real TIMESTAMP,
    CONSTRAINT uq_ciclo_anio_mes UNIQUE (anio, mes)
);

-- Catálogo de gastos fijos
CREATE TABLE gastos_fijos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(150)   NOT NULL,
    monto_actual NUMERIC(15, 2) NOT NULL,
    activo       BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Foto inmutable de gastos fijos por ciclo cerrado
CREATE TABLE gastos_fijos_snapshot (
    id               BIGSERIAL PRIMARY KEY,
    ciclo_mensual_id BIGINT         NOT NULL REFERENCES ciclos_mensuales(id),
    gasto_fijo_id    BIGINT         NOT NULL REFERENCES gastos_fijos(id),
    monto_aplicado   NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uq_snapshot_ciclo_fijo UNIQUE (ciclo_mensual_id, gasto_fijo_id)
);

-- Tarjetas de crédito
CREATE TABLE tarjetas_credito (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(100) NOT NULL,
    banco                   VARCHAR(100),
    dia_cierre_estimado     INTEGER      NOT NULL CHECK (dia_cierre_estimado BETWEEN 1 AND 31),
    dia_vencimiento_estimado INTEGER     NOT NULL CHECK (dia_vencimiento_estimado BETWEEN 1 AND 31),
    activa                  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Fechas reales de cierre por mes (override de los días estimados)
CREATE TABLE cierres_tarjeta_mes (
    id                   BIGSERIAL PRIMARY KEY,
    tarjeta_id           BIGINT NOT NULL REFERENCES tarjetas_credito(id),
    anio                 INTEGER NOT NULL,
    mes                  INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    fecha_cierre_real    DATE    NOT NULL,
    fecha_vencimiento_real DATE  NOT NULL,
    CONSTRAINT uq_cierre_tarjeta_mes UNIQUE (tarjeta_id, anio, mes)
);

-- Cabecera de compra con tarjeta
CREATE TABLE compras_tarjeta (
    id                  BIGSERIAL PRIMARY KEY,
    tarjeta_id          BIGINT         NOT NULL REFERENCES tarjetas_credito(id),
    concepto            VARCHAR(255)   NOT NULL,
    fecha_compra        DATE           NOT NULL,
    moneda_original     VARCHAR(3)     NOT NULL CHECK (moneda_original IN ('ARS', 'USD')),
    cotizacion_aplicada NUMERIC(15, 4) NOT NULL DEFAULT 1,
    monto_original      NUMERIC(15, 2) NOT NULL,
    monto_en_pesos      NUMERIC(15, 2) NOT NULL,
    cantidad_cuotas     INTEGER        NOT NULL CHECK (cantidad_cuotas >= 1)
);

-- Cuotas proyectadas a futuro por cada compra
CREATE TABLE cuotas_imputadas (
    id            BIGSERIAL PRIMARY KEY,
    compra_id     BIGINT         NOT NULL REFERENCES compras_tarjeta(id),
    numero_cuota  INTEGER        NOT NULL,
    monto_en_pesos NUMERIC(15, 2) NOT NULL,
    mes_impacto   INTEGER        NOT NULL CHECK (mes_impacto BETWEEN 1 AND 12),
    anio_impacto  INTEGER        NOT NULL,
    estado        VARCHAR(10)    NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'PAGADO'))
);

-- Categorías de gastos variables
CREATE TABLE categorias (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    icono  VARCHAR(50)
);

-- Gastos del día a día
CREATE TABLE gastos_variables (
    id               BIGSERIAL PRIMARY KEY,
    ciclo_mensual_id BIGINT         NOT NULL REFERENCES ciclos_mensuales(id),
    categoria_id     BIGINT         NOT NULL REFERENCES categorias(id),
    concepto         VARCHAR(255)   NOT NULL,
    monto            NUMERIC(15, 2) NOT NULL,
    fecha            DATE           NOT NULL
);

-- Tipos de activos de inversión
CREATE TABLE instrumentos_inversion (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo   VARCHAR(20)  NOT NULL CHECK (tipo IN ('DIVISA','CEDEAR','ACCION','CRYPTO','FCI','BONO','PLAZO_FIJO')),
    ticker VARCHAR(20),
    activo BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Movimientos de compra/venta de activos
CREATE TABLE movimientos_inversion (
    id               BIGSERIAL PRIMARY KEY,
    instrumento_id   BIGINT         NOT NULL REFERENCES instrumentos_inversion(id),
    ciclo_mensual_id BIGINT         NOT NULL REFERENCES ciclos_mensuales(id),
    tipo             VARCHAR(6)     NOT NULL CHECK (tipo IN ('COMPRA', 'VENTA')),
    fecha            DATE           NOT NULL,
    cantidad         NUMERIC(20, 6) NOT NULL,
    precio_unitario  NUMERIC(15, 4) NOT NULL,
    monto_total_pesos NUMERIC(15, 2) NOT NULL
);

-- ── Datos iniciales ──────────────────────────────────────────────────

INSERT INTO categorias (nombre, icono) VALUES
    ('Alimentación', '🍔'),
    ('Transporte', '🚌'),
    ('Entretenimiento', '🎬'),
    ('Salud', '🏥'),
    ('Indumentaria', '👕'),
    ('Educación', '📚'),
    ('Otros', '📦');

ALTER TABLE compras_tarjeta
    ADD COLUMN IF NOT EXISTS cuotas_ya_abonadas INTEGER NOT NULL DEFAULT 0;

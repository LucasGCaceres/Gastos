package com.gastos.dto.request;

import java.math.BigDecimal;

public record EfectivizarAhorroRequest(
        BigDecimal cotizacionActual,
        String nombreGasto
) {}

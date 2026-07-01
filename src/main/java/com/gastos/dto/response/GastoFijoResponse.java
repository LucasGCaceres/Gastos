package com.gastos.dto.response;

import java.math.BigDecimal;

public record GastoFijoResponse(
        Long id,
        String nombre,
        BigDecimal montoActual,
        Boolean activo
) {}

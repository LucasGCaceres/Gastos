package com.gastos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoVariableResponse(
        Long id,
        String categoria,
        String concepto,
        BigDecimal monto,
        LocalDate fecha
) {}

package com.gastos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EventoGastoResponse(
        Long id,
        String nombre,
        LocalDate fecha,
        BigDecimal total,
        List<ItemResponse> items
) {
    public record ItemResponse(Long id, String concepto, BigDecimal monto) {}
}

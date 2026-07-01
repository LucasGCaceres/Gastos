package com.gastos.dto.response;

import com.gastos.domain.enums.EstadoCiclo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CicloListItemResponse(
        Long id,
        Integer anio,
        Integer mes,
        EstadoCiclo estado,
        LocalDateTime fechaCierreReal,
        BigDecimal totalIngresos,
        BigDecimal saldoFinal
) {}

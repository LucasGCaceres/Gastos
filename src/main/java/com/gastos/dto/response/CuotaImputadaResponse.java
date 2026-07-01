package com.gastos.dto.response;

import com.gastos.domain.enums.EstadoCuota;

import java.math.BigDecimal;

public record CuotaImputadaResponse(
        Long id,
        Long compraId,
        Long tarjetaId,
        String concepto,
        String tarjeta,
        Integer numeroCuota,
        Integer totalCuotas,
        BigDecimal montoEnPesos,
        Integer mesImpacto,
        Integer anioImpacto,
        EstadoCuota estado
) {}

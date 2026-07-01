package com.gastos.dto.response;

import com.gastos.domain.enums.EstadoCiclo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CicloResumenResponse(
        Long id,
        Integer anio,
        Integer mes,
        EstadoCiclo estado,
        LocalDateTime fechaCierreReal,
        BigDecimal totalIngresos,
        BigDecimal totalFijos,
        BigDecimal totalVariables,
        BigDecimal totalCuotas,
        BigDecimal saldoFinal,
        List<GastoFijoItemResponse> gastosFijos,
        List<GastoVariableResponse> gastosVariables,
        List<CuotaImputadaResponse> cuotas
) {
    public record GastoFijoItemResponse(Long id, String nombre, BigDecimal monto) {}
}

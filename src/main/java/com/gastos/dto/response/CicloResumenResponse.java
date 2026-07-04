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
        List<CuotaImputadaResponse> cuotas,
        List<GastoEsperadoItemResponse> gastosEsperados,
        List<EventoGastoResponse> eventos
) {
    public record GastoFijoItemResponse(Long id, String nombre, BigDecimal monto) {}

    /**
     * Gasto proyectado (no consolidado) originado en una calculadora de Ahorro USD con
     * "Guardar". El equivalente en pesos NO se persiste: se recalcula en el cliente con la
     * cotización vigente, hasta que el usuario decide "Efectivizar" (ahí sí se vuelve un
     * GastoVariable real, cerrado, con la cotización congelada al momento de la operación).
     */
    public record GastoEsperadoItemResponse(Long calculadoraId, String nombre, BigDecimal objetivoUsd) {}
}

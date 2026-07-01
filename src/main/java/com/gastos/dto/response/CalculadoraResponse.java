package com.gastos.dto.response;

import com.gastos.domain.enums.TipoCalculadora;

import java.math.BigDecimal;
import java.util.List;

public record CalculadoraResponse(
        Long id,
        String nombre,
        TipoCalculadora tipo,
        Long gastoFijoId,
        String gastoFijoNombre,
        BigDecimal totalCalculado,
        List<ItemResponse> items,
        List<RutaResponse> rutas,
        AhorroData ahorro
) {
    public record ItemResponse(
            Long id, String nombre, BigDecimal cantidad,
            BigDecimal precioUnitario, BigDecimal subtotal
    ) {}

    public record RutaResponse(
            Long id, String nombre, Integer viajesPorMes,
            List<TramoResponse> tramos, BigDecimal costoMensual
    ) {}

    public record TramoResponse(
            Long id, String nombre, BigDecimal precio,
            Integer orden, BigDecimal factor, BigDecimal precioEfectivo
    ) {}

    public record AhorroData(
            BigDecimal objetivoUsd,
            List<OperacionData> historial
    ) {}

    public record OperacionData(
            Long id,
            BigDecimal objetivoUsd,
            BigDecimal cotizacion,
            BigDecimal montoArs,
            String concepto,
            String fecha,
            Integer mesImpacto,
            Integer anioImpacto
    ) {}
}

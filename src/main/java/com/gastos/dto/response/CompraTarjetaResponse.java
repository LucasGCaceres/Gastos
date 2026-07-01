package com.gastos.dto.response;

import com.gastos.domain.enums.Moneda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraTarjetaResponse(
        Long id,
        Long tarjetaId,
        String tarjeta,
        String concepto,
        LocalDate fechaCompra,
        Moneda monedaOriginal,
        BigDecimal montoOriginal,
        BigDecimal cotizacionAplicada,
        BigDecimal montoEnPesos,
        Integer cantidadCuotas,
        Integer cuotasYaAbonadas,
        List<CuotaImputadaResponse> cuotas
) {}

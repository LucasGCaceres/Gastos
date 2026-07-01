package com.gastos.dto.response;

import com.gastos.domain.enums.TipoMovimientoInversion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoInversionResponse(
        Long id,
        Long instrumentoId,
        String instrumento,
        String tipoInstrumento,
        TipoMovimientoInversion tipo,
        LocalDate fecha,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal montoTotalPesos
) {}

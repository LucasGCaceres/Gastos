package com.gastos.dto.request;

import com.gastos.domain.enums.TipoMovimientoInversion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarMovimientoRequest(
        @NotNull Long instrumentoId,
        @NotNull Long cicloMensualId,
        @NotNull TipoMovimientoInversion tipo,
        @NotNull LocalDate fecha,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @Positive BigDecimal precioUnitario
) {}

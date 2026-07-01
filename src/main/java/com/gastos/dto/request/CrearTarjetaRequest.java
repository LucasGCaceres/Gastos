package com.gastos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearTarjetaRequest(
        @NotBlank String nombre,
        String banco,
        @NotNull @Min(1) @Max(31) Integer diaCierreEstimado,
        @NotNull @Min(1) @Max(31) Integer diaVencimientoEstimado
) {}

package com.gastos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearCicloRequest(
        @NotNull @Min(2000) @Max(2100) Integer anio,
        @NotNull @Min(1) @Max(12) Integer mes
) {}

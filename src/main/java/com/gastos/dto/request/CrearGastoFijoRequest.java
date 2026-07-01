package com.gastos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearGastoFijoRequest(
        @NotBlank String nombre,
        @NotNull @DecimalMin("0.01") BigDecimal montoActual
) {}

package com.gastos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AgregarItemEventoRequest(
        @NotBlank String concepto,
        @NotNull @DecimalMin("0.01") BigDecimal monto
) {}

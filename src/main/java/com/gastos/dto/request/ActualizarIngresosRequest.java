package com.gastos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActualizarIngresosRequest(
        @NotNull @DecimalMin("0.00") BigDecimal totalIngresos
) {}

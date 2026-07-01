package com.gastos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarGastoVariableRequest(
        @NotNull Long categoriaId,
        @NotBlank String concepto,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull LocalDate fecha
) {}

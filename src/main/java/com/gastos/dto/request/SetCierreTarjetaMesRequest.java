package com.gastos.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SetCierreTarjetaMesRequest(
        @NotNull Integer anio,
        @NotNull Integer mes,
        @NotNull LocalDate fechaCierreReal,
        @NotNull LocalDate fechaVencimientoReal
) {}

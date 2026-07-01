package com.gastos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ActualizarCalculadoraRutasRequest(
        @NotNull List<Ruta> rutas
) {
    public record Ruta(
            @NotBlank String nombre,
            @NotNull @Positive Integer viajesPorMes,
            @NotNull List<Tramo> tramos
    ) {}

    public record Tramo(
            @NotBlank String nombre,
            @NotNull @Positive BigDecimal precio
    ) {}
}

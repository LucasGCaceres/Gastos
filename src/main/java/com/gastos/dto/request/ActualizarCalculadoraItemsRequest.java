package com.gastos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ActualizarCalculadoraItemsRequest(
        @NotNull List<Item> items
) {
    public record Item(
            @NotBlank String nombre,
            @NotNull @Positive BigDecimal cantidad,
            @NotNull @Positive BigDecimal precioUnitario
    ) {}
}

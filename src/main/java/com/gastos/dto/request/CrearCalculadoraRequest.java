package com.gastos.dto.request;

import com.gastos.domain.enums.TipoCalculadora;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearCalculadoraRequest(
        @NotBlank String nombre,
        @NotNull TipoCalculadora tipo,
        Long gastoFijoId,
        String nuevoGastoFijoNombre
) {}

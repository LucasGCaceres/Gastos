package com.gastos.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoGastoFijoRequest(
        @NotNull Boolean activo
) {}

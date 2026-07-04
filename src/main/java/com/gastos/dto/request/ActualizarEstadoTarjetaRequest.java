package com.gastos.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoTarjetaRequest(
        @NotNull Boolean activa
) {}

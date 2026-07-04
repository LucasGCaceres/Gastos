package com.gastos.dto.request;

import com.gastos.domain.enums.EstadoCuota;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoCuotaRequest(
        @NotNull EstadoCuota estado
) {}

package com.gastos.dto.response;

import java.math.BigDecimal;

public record TarjetaDeudaResponse(
        Long tarjetaId,
        String nombre,
        String banco,
        BigDecimal totalAdeudado
) {}

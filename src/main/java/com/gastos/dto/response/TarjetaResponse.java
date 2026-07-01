package com.gastos.dto.response;

public record TarjetaResponse(
        Long id,
        String nombre,
        String banco,
        Integer diaCierreEstimado,
        Integer diaVencimientoEstimado,
        Boolean activa
) {}

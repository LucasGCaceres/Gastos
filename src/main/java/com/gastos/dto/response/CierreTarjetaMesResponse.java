package com.gastos.dto.response;

import java.time.LocalDate;

public record CierreTarjetaMesResponse(
        Long id,
        Long tarjetaId,
        Integer anio,
        Integer mes,
        LocalDate fechaCierreReal,
        LocalDate fechaVencimientoReal
) {}

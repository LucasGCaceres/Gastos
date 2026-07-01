package com.gastos.dto.response;

import com.gastos.domain.enums.TipoInstrumento;

import java.math.BigDecimal;
import java.util.List;

public record PatrimonioResponse(
        BigDecimal totalInvertidoPesos,
        List<PosicionResponse> posiciones
) {
    public record PosicionResponse(
            Long instrumentoId,
            String nombre,
            String ticker,
            TipoInstrumento tipo,
            BigDecimal cantidadNeta,
            BigDecimal costoPromedioUnitario,
            BigDecimal costoTotalPesos
    ) {}
}

package com.gastos.dto.request;

import com.gastos.domain.enums.Moneda;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrarCompraRequest(
        @NotNull Long tarjetaId,
        @NotBlank String concepto,
        @NotNull LocalDate fechaCompra,
        @NotNull Moneda monedaOriginal,
        // Requerido si monedaOriginal es USD
        BigDecimal cotizacionAplicada,
        @NotNull @DecimalMin("0.01") BigDecimal montoOriginal,
        @NotNull @Min(1) Integer cantidadCuotas,
        @Min(0) Integer cuotasYaAbonadas
) {}

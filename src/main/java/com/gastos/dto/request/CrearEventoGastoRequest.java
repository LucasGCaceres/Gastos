package com.gastos.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CrearEventoGastoRequest(
        @NotBlank String nombre,
        LocalDate fecha
) {}

package com.gastos.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CrearCategoriaRequest(
        @NotBlank String nombre,
        String icono
) {}

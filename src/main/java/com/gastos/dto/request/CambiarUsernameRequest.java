package com.gastos.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarUsernameRequest(
        @NotBlank String newUsername,
        @NotBlank String currentPassword
) {}

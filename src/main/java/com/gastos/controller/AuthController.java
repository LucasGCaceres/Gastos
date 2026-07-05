package com.gastos.controller;

import com.gastos.dto.request.CambiarPasswordRequest;
import com.gastos.dto.request.LoginRequest;
import com.gastos.dto.response.LoginResponse;
import com.gastos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(Authentication authentication, @Valid @RequestBody CambiarPasswordRequest req) {
        authService.cambiarPassword(authentication.getName(), req);
    }
}

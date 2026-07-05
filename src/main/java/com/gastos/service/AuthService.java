package com.gastos.service;

import com.gastos.domain.repository.UsuarioRepository;
import com.gastos.dto.request.CambiarPasswordRequest;
import com.gastos.dto.request.LoginRequest;
import com.gastos.dto.response.LoginResponse;
import com.gastos.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        var usuario = usuarioRepo.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generarToken(usuario.getUsername());
        return new LoginResponse(token, usuario.getUsername());
    }

    @Transactional
    public void cambiarPassword(String username, CambiarPasswordRequest req) {
        var usuario = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + username));

        if (!passwordEncoder.matches(req.currentPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta");
        }

        usuario.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        usuarioRepo.save(usuario);
    }
}

package com.gastos.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-days}") long expirationDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret no está configurado. Definí la variable de entorno JWT_SECRET.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
    }

    public String generarToken(String username) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expirationDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    /** Devuelve el username si el token es válido, o null si es inválido/expirado. */
    public String validarYObtenerUsername(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}

package com.gastos.security;

import com.gastos.domain.model.Usuario;
import com.gastos.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario administrador en el primer arranque, a partir de ADMIN_USERNAME/ADMIN_PASSWORD.
 * No hace nada si ya existe algún usuario, para no pisar la contraseña en arranques siguientes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() > 0) return;

        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("No hay usuarios y ADMIN_USERNAME/ADMIN_PASSWORD no están configurados: " +
                    "no se pudo crear el usuario inicial. Definí esas variables de entorno y reiniciá.");
            return;
        }

        usuarioRepo.save(Usuario.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build());
        log.info("Usuario administrador '{}' creado.", adminUsername);
    }
}

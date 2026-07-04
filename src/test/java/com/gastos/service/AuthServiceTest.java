package com.gastos.service;

import com.gastos.domain.model.Usuario;
import com.gastos.domain.repository.UsuarioRepository;
import com.gastos.dto.request.LoginRequest;
import com.gastos.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).username("lucas").passwordHash("hash").build();
    }

    @Test
    void loginConCredencialesCorrectas_devuelveToken() {
        when(usuarioRepo.findByUsername("lucas")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-correcta", "hash")).thenReturn(true);
        when(jwtService.generarToken("lucas")).thenReturn("token-generado");

        var response = service.login(new LoginRequest("lucas", "clave-correcta"));

        assertThat(response.token()).isEqualTo("token-generado");
        assertThat(response.username()).isEqualTo("lucas");
    }

    @Test
    void loginConClaveIncorrecta_lanzaBadCredentials() {
        when(usuarioRepo.findByUsername("lucas")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "clave-mala")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginConUsuarioInexistente_lanzaBadCredentials() {
        when(usuarioRepo.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("desconocido", "cualquiera")))
                .isInstanceOf(BadCredentialsException.class);
    }
}

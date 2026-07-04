package com.gastos.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService service = new JwtService(
            "clave-de-prueba-suficientemente-larga-para-hs256-1234567890", 30);

    @Test
    void generarToken_yValidarlo_devuelveElMismoUsername() {
        String token = service.generarToken("lucas");

        assertThat(service.validarYObtenerUsername(token)).isEqualTo("lucas");
    }

    @Test
    void tokenInvalido_devuelveNull() {
        assertThat(service.validarYObtenerUsername("esto-no-es-un-jwt")).isNull();
    }

    @Test
    void tokenFirmadoConOtraClave_devuelveNull() {
        JwtService otroService = new JwtService(
                "otra-clave-completamente-distinta-tambien-larga-9876543210", 30);
        String token = otroService.generarToken("lucas");

        assertThat(service.validarYObtenerUsername(token)).isNull();
    }

    @Test
    void secretoVacio_lanzaExcepcionAlConstruir() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtService("", 30))
                .isInstanceOf(IllegalStateException.class);
    }
}

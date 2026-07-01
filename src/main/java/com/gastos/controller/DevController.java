package com.gastos.controller;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints exclusivos del perfil "dev" para resetear datos de prueba.
 * NO se activan en producción.
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("!prod")
public class DevController {

    private final EntityManager em;

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void resetearBaseDeDatos() {
        // TRUNCATE CASCADE elimina todo en una pasada sin violar FK constraints,
        // y RESTART IDENTITY resetea las secuencias de los ids.
        em.createNativeQuery("""
            TRUNCATE TABLE
                ahorro_operaciones,
                calculadora_ahorro,
                calculadora_tramos,
                calculadora_rutas,
                calculadora_items,
                calculadoras,
                cuotas_imputadas,
                compras_tarjeta,
                cierres_tarjeta_mes,
                tarjetas_credito,
                gasto_fijo_snapshots,
                gastos_variables,
                gastos_fijos,
                ciclos_mensuales,
                movimientos_inversion,
                instrumentos_inversion
            RESTART IDENTITY CASCADE
            """).executeUpdate();
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("env", "dev", "reset", "POST /api/dev/reset");
    }
}

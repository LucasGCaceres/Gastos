package com.gastos.controller;

import com.gastos.dto.request.*;
import com.gastos.dto.response.CalculadoraResponse;
import com.gastos.service.CalculadoraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calculadoras")
@RequiredArgsConstructor
public class CalculadoraController {

    private final CalculadoraService calculadoraService;

    @GetMapping
    public List<CalculadoraResponse> listar() {
        return calculadoraService.listar();
    }

    @GetMapping("/{id}")
    public CalculadoraResponse obtener(@PathVariable Long id) {
        return calculadoraService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalculadoraResponse crear(@Valid @RequestBody CrearCalculadoraRequest req) {
        return calculadoraService.crear(req);
    }

    @PutMapping("/{id}/items")
    public CalculadoraResponse actualizarItems(@PathVariable Long id,
                                               @Valid @RequestBody ActualizarCalculadoraItemsRequest req) {
        return calculadoraService.actualizarItems(id, req);
    }

    @PutMapping("/{id}/rutas")
    public CalculadoraResponse actualizarRutas(@PathVariable Long id,
                                               @Valid @RequestBody ActualizarCalculadoraRutasRequest req) {
        return calculadoraService.actualizarRutas(id, req);
    }

    @PutMapping("/{id}/ahorro")
    public CalculadoraResponse actualizarAhorro(@PathVariable Long id,
                                                @RequestBody ActualizarCalculadoraAhorroRequest req) {
        return calculadoraService.actualizarAhorro(id, req);
    }

    @PostMapping("/{id}/efectivizar")
    public CalculadoraResponse efectivizar(@PathVariable Long id,
                                           @RequestBody EfectivizarAhorroRequest req) {
        return calculadoraService.efectivizarAhorro(id, req);
    }

    @PostMapping("/{id}/aplicar")
    public CalculadoraResponse aplicar(@PathVariable Long id) {
        return calculadoraService.aplicar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        calculadoraService.desactivar(id);
    }
}

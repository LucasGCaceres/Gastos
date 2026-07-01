package com.gastos.controller;

import com.gastos.dto.request.CrearGastoFijoRequest;
import com.gastos.dto.response.GastoFijoResponse;
import com.gastos.service.GastoFijoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/gastos-fijos")
@RequiredArgsConstructor
public class GastoFijoController {

    private final GastoFijoService gastoFijoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoFijoResponse crear(@Valid @RequestBody CrearGastoFijoRequest req) {
        return gastoFijoService.crear(req);
    }

    @GetMapping
    public List<GastoFijoResponse> listar() {
        return gastoFijoService.listarActivos();
    }

    @PatchMapping("/{id}")
    public GastoFijoResponse editar(@PathVariable Long id,
                                    @Valid @RequestBody CrearGastoFijoRequest req) {
        return gastoFijoService.editar(id, req);
    }

    @PatchMapping("/{id}/monto")
    public GastoFijoResponse actualizarMonto(@PathVariable Long id,
                                              @RequestParam BigDecimal monto) {
        return gastoFijoService.actualizarMonto(id, monto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        gastoFijoService.desactivar(id);
    }
}

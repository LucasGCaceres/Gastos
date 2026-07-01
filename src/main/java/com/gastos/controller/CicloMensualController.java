package com.gastos.controller;

import com.gastos.dto.request.ActualizarIngresosRequest;
import com.gastos.dto.request.CrearCicloRequest;
import com.gastos.dto.response.CicloListItemResponse;
import com.gastos.dto.response.CicloResumenResponse;
import com.gastos.service.CicloMensualService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciclos")
@RequiredArgsConstructor
public class CicloMensualController {

    private final CicloMensualService cicloService;

    @GetMapping
    public List<CicloListItemResponse> listar() {
        return cicloService.listarTodos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CicloResumenResponse crear(@Valid @RequestBody CrearCicloRequest req) {
        var ciclo = cicloService.crearCiclo(req);
        return cicloService.obtenerResumen(ciclo.getId());
    }

    @GetMapping("/{anio}/{mes}")
    public CicloResumenResponse obtenerPorAnioMes(@PathVariable Integer anio, @PathVariable Integer mes) {
        var ciclo = cicloService.obtenerPorAnioMes(anio, mes);
        return cicloService.obtenerResumen(ciclo.getId());
    }

    @GetMapping("/{id}/resumen")
    public CicloResumenResponse obtenerResumen(@PathVariable Long id) {
        return cicloService.obtenerResumen(id);
    }

    @PatchMapping("/{id}/ingresos")
    public CicloResumenResponse actualizarIngresos(@PathVariable Long id,
                                                    @Valid @RequestBody ActualizarIngresosRequest req) {
        cicloService.actualizarIngresos(id, req);
        return cicloService.obtenerResumen(id);
    }

    @PostMapping("/{id}/cerrar")
    public CicloResumenResponse cerrar(@PathVariable Long id) {
        return cicloService.cerrarCiclo(id);
    }

    @PostMapping("/{id}/reabrir")
    public CicloResumenResponse reabrir(@PathVariable Long id) {
        return cicloService.reabrirCiclo(id);
    }
}

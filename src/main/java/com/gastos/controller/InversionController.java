package com.gastos.controller;

import com.gastos.dto.request.RegistrarMovimientoRequest;
import com.gastos.dto.response.MovimientoInversionResponse;
import com.gastos.dto.response.PatrimonioResponse;
import com.gastos.service.InversionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InversionController {

    private final InversionService inversionService;

    @PostMapping("/api/inversiones")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInversionResponse registrar(@Valid @RequestBody RegistrarMovimientoRequest req) {
        return inversionService.registrar(req);
    }

    @GetMapping("/api/ciclos/{cicloId}/inversiones")
    public List<MovimientoInversionResponse> listarPorCiclo(@PathVariable Long cicloId) {
        return inversionService.listarPorCiclo(cicloId);
    }

    @GetMapping("/api/patrimonio")
    public PatrimonioResponse calcularPatrimonio() {
        return inversionService.calcularPatrimonio();
    }
}

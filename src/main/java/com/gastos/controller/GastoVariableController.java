package com.gastos.controller;

import com.gastos.dto.request.RegistrarGastoVariableRequest;
import com.gastos.dto.response.GastoVariableResponse;
import com.gastos.service.GastoVariableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

@RestController
@RequestMapping("/api/ciclos/{cicloId}/gastos-variables")
@RequiredArgsConstructor
public class GastoVariableController {

    private final GastoVariableService gastoVariableService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoVariableResponse registrar(@PathVariable Long cicloId,
                                            @Valid @RequestBody RegistrarGastoVariableRequest req) {
        return gastoVariableService.registrar(cicloId, req);
    }

    @GetMapping
    public List<GastoVariableResponse> listar(@PathVariable Long cicloId) {
        return gastoVariableService.listar(cicloId);
    }

    @PatchMapping("/{gastoId}")
    public GastoVariableResponse editar(@PathVariable Long cicloId, @PathVariable Long gastoId,
                                         @Valid @RequestBody RegistrarGastoVariableRequest req) {
        return gastoVariableService.editar(cicloId, gastoId, req);
    }

    @DeleteMapping("/{gastoId}")
    @ResponseStatus(NO_CONTENT)
    public void eliminar(@PathVariable Long cicloId, @PathVariable Long gastoId) {
        gastoVariableService.eliminar(cicloId, gastoId);
    }
}

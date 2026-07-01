package com.gastos.controller;

import com.gastos.dto.request.RegistrarCompraRequest;
import com.gastos.dto.response.CompraTarjetaResponse;
import com.gastos.service.CompraTarjetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras-tarjeta")
@RequiredArgsConstructor
public class CompraTarjetaController {

    private final CompraTarjetaService compraService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraTarjetaResponse registrar(@Valid @RequestBody RegistrarCompraRequest req) {
        return compraService.registrarCompra(req);
    }

    @GetMapping("/{id}")
    public CompraTarjetaResponse obtener(@PathVariable Long id) {
        return compraService.obtener(id);
    }

    @PutMapping("/{id}")
    public CompraTarjetaResponse editar(@PathVariable Long id, @Valid @RequestBody RegistrarCompraRequest req) {
        return compraService.editarCompra(id, req);
    }

    @GetMapping("/tarjeta/{tarjetaId}")
    public List<CompraTarjetaResponse> listarPorTarjeta(@PathVariable Long tarjetaId) {
        return compraService.listarPorTarjeta(tarjetaId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        compraService.eliminarCompra(id);
    }
}

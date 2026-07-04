package com.gastos.controller;

import com.gastos.dto.request.AgregarItemEventoRequest;
import com.gastos.dto.request.CrearEventoGastoRequest;
import com.gastos.dto.response.EventoGastoResponse;
import com.gastos.service.EventoGastoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventoGastoController {

    private final EventoGastoService eventoService;

    @PostMapping("/api/ciclos/{cicloId}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoGastoResponse crear(@PathVariable Long cicloId, @Valid @RequestBody CrearEventoGastoRequest req) {
        return eventoService.crear(cicloId, req);
    }

    @GetMapping("/api/ciclos/{cicloId}/eventos")
    public List<EventoGastoResponse> listar(@PathVariable Long cicloId) {
        return eventoService.listar(cicloId);
    }

    @PostMapping("/api/eventos/{eventoId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoGastoResponse agregarItem(@PathVariable Long eventoId, @Valid @RequestBody AgregarItemEventoRequest req) {
        return eventoService.agregarItem(eventoId, req);
    }

    @PatchMapping("/api/eventos/{eventoId}/items/{itemId}")
    public EventoGastoResponse editarItem(@PathVariable Long eventoId, @PathVariable Long itemId,
                                           @Valid @RequestBody AgregarItemEventoRequest req) {
        return eventoService.editarItem(eventoId, itemId, req);
    }

    @DeleteMapping("/api/eventos/{eventoId}/items/{itemId}")
    public EventoGastoResponse eliminarItem(@PathVariable Long eventoId, @PathVariable Long itemId) {
        return eventoService.eliminarItem(eventoId, itemId);
    }

    @DeleteMapping("/api/eventos/{eventoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvento(@PathVariable Long eventoId) {
        eventoService.eliminarEvento(eventoId);
    }
}

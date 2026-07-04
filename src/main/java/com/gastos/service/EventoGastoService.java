package com.gastos.service;

import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.EventoGasto;
import com.gastos.domain.model.EventoGastoItem;
import com.gastos.domain.repository.EventoGastoItemRepository;
import com.gastos.domain.repository.EventoGastoRepository;
import com.gastos.dto.request.AgregarItemEventoRequest;
import com.gastos.dto.request.CrearEventoGastoRequest;
import com.gastos.dto.response.EventoGastoResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoGastoService {

    private final EventoGastoRepository eventoRepo;
    private final EventoGastoItemRepository itemRepo;
    private final CicloMensualService cicloService;

    @Transactional
    public EventoGastoResponse crear(Long cicloId, CrearEventoGastoRequest req) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        cicloService.validarAbierto(ciclo);

        EventoGasto evento = EventoGasto.builder()
                .cicloMensual(ciclo)
                .nombre(req.nombre())
                .fecha(req.fecha() != null ? req.fecha() : LocalDate.now())
                .build();
        EventoGastoResponse response = toResponse(eventoRepo.save(evento));
        cicloService.recalcularTotales(cicloId);
        return response;
    }

    @Transactional(readOnly = true)
    public List<EventoGastoResponse> listar(Long cicloId) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        return eventoRepo.findByCicloMensualOrderByIdDesc(ciclo).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventoGastoResponse agregarItem(Long eventoId, AgregarItemEventoRequest req) {
        EventoGasto evento = get(eventoId);
        cicloService.validarAbierto(evento.getCicloMensual());

        int orden = evento.getItems().stream().mapToInt(EventoGastoItem::getOrden).max().orElse(-1) + 1;
        evento.getItems().add(EventoGastoItem.builder()
                .evento(evento)
                .concepto(req.concepto())
                .monto(req.monto())
                .orden(orden)
                .build());

        EventoGastoResponse response = toResponse(eventoRepo.save(evento));
        cicloService.recalcularTotales(evento.getCicloMensual().getId());
        return response;
    }

    @Transactional
    public EventoGastoResponse editarItem(Long eventoId, Long itemId, AgregarItemEventoRequest req) {
        EventoGasto evento = get(eventoId);
        cicloService.validarAbierto(evento.getCicloMensual());

        EventoGastoItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Ítem no encontrado: " + itemId));
        if (!item.getEvento().getId().equals(eventoId)) {
            throw new IllegalArgumentException("El ítem no pertenece al evento indicado");
        }
        item.setConcepto(req.concepto());
        item.setMonto(req.monto());
        itemRepo.save(item);

        EventoGastoResponse response = toResponse(get(eventoId));
        cicloService.recalcularTotales(evento.getCicloMensual().getId());
        return response;
    }

    @Transactional
    public EventoGastoResponse eliminarItem(Long eventoId, Long itemId) {
        EventoGasto evento = get(eventoId);
        cicloService.validarAbierto(evento.getCicloMensual());

        EventoGastoItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Ítem no encontrado: " + itemId));
        if (!item.getEvento().getId().equals(eventoId)) {
            throw new IllegalArgumentException("El ítem no pertenece al evento indicado");
        }
        evento.getItems().remove(item);

        EventoGastoResponse response = toResponse(eventoRepo.save(evento));
        cicloService.recalcularTotales(evento.getCicloMensual().getId());
        return response;
    }

    @Transactional
    public void eliminarEvento(Long eventoId) {
        EventoGasto evento = get(eventoId);
        cicloService.validarAbierto(evento.getCicloMensual());
        Long cicloId = evento.getCicloMensual().getId();
        eventoRepo.delete(evento);
        cicloService.recalcularTotales(cicloId);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private EventoGasto get(Long id) {
        return eventoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado: " + id));
    }

    private EventoGastoResponse toResponse(EventoGasto evento) {
        List<EventoGastoResponse.ItemResponse> items = evento.getItems().stream()
                .map(i -> new EventoGastoResponse.ItemResponse(i.getId(), i.getConcepto(), i.getMonto()))
                .toList();
        BigDecimal total = evento.getItems().stream()
                .map(EventoGastoItem::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new EventoGastoResponse(evento.getId(), evento.getNombre(), evento.getFecha(), total, items);
    }
}

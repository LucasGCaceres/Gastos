package com.gastos.service;

import com.gastos.domain.model.GastoFijo;
import com.gastos.domain.repository.GastoFijoRepository;
import com.gastos.dto.request.CrearGastoFijoRequest;
import com.gastos.dto.response.GastoFijoResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoFijoService {

    private final GastoFijoRepository gastoFijoRepo;

    @Transactional
    public GastoFijoResponse crear(CrearGastoFijoRequest req) {
        GastoFijo gasto = GastoFijo.builder()
                .nombre(req.nombre())
                .montoActual(req.montoActual())
                .build();
        return toResponse(gastoFijoRepo.save(gasto));
    }

    @Transactional(readOnly = true)
    public List<GastoFijoResponse> listarActivos() {
        return gastoFijoRepo.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    @Transactional
    public GastoFijoResponse editar(Long id, CrearGastoFijoRequest req) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        gasto.setNombre(req.nombre());
        gasto.setMontoActual(req.montoActual());
        return toResponse(gastoFijoRepo.save(gasto));
    }

    @Transactional
    public GastoFijoResponse actualizarMonto(Long id, BigDecimal nuevoMonto) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        gasto.setMontoActual(nuevoMonto);
        return toResponse(gastoFijoRepo.save(gasto));
    }

    @Transactional
    public void desactivar(Long id) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        gasto.setActivo(false);
        gastoFijoRepo.save(gasto);
    }

    private GastoFijoResponse toResponse(GastoFijo g) {
        return new GastoFijoResponse(g.getId(), g.getNombre(), g.getMontoActual(), g.getActivo());
    }
}

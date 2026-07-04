package com.gastos.service;

import com.gastos.domain.enums.EstadoCiclo;
import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.GastoFijo;
import com.gastos.domain.repository.CicloMensualRepository;
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
    private final CicloMensualRepository cicloRepo;
    private final CicloMensualService cicloMensualService;

    @Transactional
    public GastoFijoResponse crear(CrearGastoFijoRequest req) {
        GastoFijo gasto = GastoFijo.builder()
                .nombre(req.nombre())
                .montoActual(req.montoActual())
                .build();
        GastoFijoResponse response = toResponse(gastoFijoRepo.save(gasto));
        recalcularCiclosAbiertos();
        return response;
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
        GastoFijoResponse response = toResponse(gastoFijoRepo.save(gasto));
        recalcularCiclosAbiertos();
        return response;
    }

    @Transactional
    public GastoFijoResponse actualizarMonto(Long id, BigDecimal nuevoMonto) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        gasto.setMontoActual(nuevoMonto);
        GastoFijoResponse response = toResponse(gastoFijoRepo.save(gasto));
        recalcularCiclosAbiertos();
        return response;
    }

    @Transactional
    public GastoFijoResponse actualizarEstado(Long id, boolean activo) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        gasto.setActivo(activo);
        GastoFijoResponse response = toResponse(gastoFijoRepo.save(gasto));
        recalcularCiclosAbiertos();
        return response;
    }

    @Transactional
    public void desactivar(Long id) {
        actualizarEstado(id, false);
    }

    /**
     * Asegura que el gasto fijo esté activo, sin tocar nombre/monto.
     * Usado al aplicar una calculadora: si el gasto estaba desactivado, el total
     * recalculado quedaba guardado pero invisible en los ciclos (findByActivoTrue lo excluía).
     */
    @Transactional
    public void activar(Long id) {
        GastoFijo gasto = gastoFijoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + id));
        if (!Boolean.TRUE.equals(gasto.getActivo())) {
            gasto.setActivo(true);
            gastoFijoRepo.save(gasto);
        }
    }

    private void recalcularCiclosAbiertos() {
        List<CicloMensual> abiertos = cicloRepo.findByEstadoOrderByAnioDescMesDesc(EstadoCiclo.ABIERTO);
        abiertos.forEach(c -> cicloMensualService.recalcularTotales(c.getId()));
    }

    private GastoFijoResponse toResponse(GastoFijo g) {
        return new GastoFijoResponse(g.getId(), g.getNombre(), g.getMontoActual(), g.getActivo());
    }
}

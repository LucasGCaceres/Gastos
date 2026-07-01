package com.gastos.service;

import com.gastos.domain.model.Categoria;
import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.GastoVariable;
import com.gastos.domain.repository.CategoriaRepository;
import com.gastos.domain.repository.GastoVariableRepository;
import com.gastos.dto.request.RegistrarGastoVariableRequest;
import com.gastos.dto.response.GastoVariableResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoVariableService {

    private final GastoVariableRepository gastoVariableRepo;
    private final CategoriaRepository categoriaRepo;
    private final CicloMensualService cicloService;

    @Transactional
    public GastoVariableResponse registrar(Long cicloId, RegistrarGastoVariableRequest req) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        cicloService.validarAbierto(ciclo);
        Categoria categoria = categoriaRepo.findById(req.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + req.categoriaId()));

        GastoVariable gasto = GastoVariable.builder()
                .cicloMensual(ciclo)
                .categoria(categoria)
                .concepto(req.concepto())
                .monto(req.monto())
                .fecha(req.fecha())
                .build();

        GastoVariable saved = gastoVariableRepo.save(gasto);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GastoVariableResponse> listar(Long cicloId) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        return gastoVariableRepo.findByCicloMensualOrderByFechaDesc(ciclo)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public GastoVariableResponse editar(Long cicloId, Long gastoId, RegistrarGastoVariableRequest req) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        cicloService.validarAbierto(ciclo);
        GastoVariable gasto = gastoVariableRepo.findById(gastoId)
                .orElseThrow(() -> new EntityNotFoundException("Gasto variable no encontrado: " + gastoId));
        if (!gasto.getCicloMensual().getId().equals(cicloId)) {
            throw new IllegalArgumentException("El gasto no pertenece al ciclo indicado");
        }
        Categoria categoria = categoriaRepo.findById(req.categoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + req.categoriaId()));
        gasto.setCategoria(categoria);
        gasto.setConcepto(req.concepto());
        gasto.setMonto(req.monto());
        gasto.setFecha(req.fecha());
        return toResponse(gastoVariableRepo.save(gasto));
    }

    @Transactional
    public void eliminar(Long cicloId, Long gastoId) {
        CicloMensual ciclo = cicloService.obtenerPorId(cicloId);
        cicloService.validarAbierto(ciclo);
        GastoVariable gasto = gastoVariableRepo.findById(gastoId)
                .orElseThrow(() -> new EntityNotFoundException("Gasto variable no encontrado: " + gastoId));
        if (!gasto.getCicloMensual().getId().equals(cicloId)) {
            throw new IllegalArgumentException("El gasto no pertenece al ciclo indicado");
        }
        gastoVariableRepo.delete(gasto);
    }

    private GastoVariableResponse toResponse(GastoVariable g) {
        return new GastoVariableResponse(
                g.getId(), g.getCategoria().getNombre(),
                g.getConcepto(), g.getMonto(), g.getFecha());
    }
}

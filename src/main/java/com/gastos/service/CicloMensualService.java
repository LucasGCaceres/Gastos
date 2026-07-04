package com.gastos.service;

import com.gastos.domain.enums.EstadoCiclo;
import com.gastos.domain.enums.TipoCalculadora;
import com.gastos.domain.model.*;
import com.gastos.domain.repository.*;
import com.gastos.dto.request.ActualizarIngresosRequest;
import com.gastos.dto.request.CrearCicloRequest;
import com.gastos.dto.response.CicloListItemResponse;
import com.gastos.dto.response.CicloResumenResponse;
import com.gastos.dto.response.CuotaImputadaResponse;
import com.gastos.dto.response.EventoGastoResponse;
import com.gastos.dto.response.GastoVariableResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CicloMensualService {

    private final CicloMensualRepository cicloRepo;
    private final GastoFijoRepository gastoFijoRepo;
    private final GastoFijoSnapshotRepository snapshotRepo;
    private final GastoVariableRepository gastoVariableRepo;
    private final CuotaImputadaRepository cuotaRepo;
    private final CalculadoraRepository calculadoraRepo;
    private final EventoGastoRepository eventoGastoRepo;

    @Transactional(readOnly = true)
    public List<CicloListItemResponse> listarTodos() {
        return cicloRepo.findAllByOrderByAnioDescMesDesc().stream()
                .map(c -> new CicloListItemResponse(
                        c.getId(), c.getAnio(), c.getMes(), c.getEstado(),
                        c.getFechaCierreReal(), c.getTotalIngresos(), c.getSaldoFinal()))
                .toList();
    }

    @Transactional
    public CicloMensual crearCiclo(CrearCicloRequest req) {
        if (cicloRepo.existsByAnioAndMes(req.anio(), req.mes())) {
            throw new IllegalStateException("Ya existe un ciclo para %d/%d".formatted(req.mes(), req.anio()));
        }
        return cicloRepo.save(CicloMensual.builder()
                .anio(req.anio())
                .mes(req.mes())
                .build());
    }

    @Transactional
    public CicloMensual actualizarIngresos(Long cicloId, ActualizarIngresosRequest req) {
        CicloMensual ciclo = obtenerPorId(cicloId);
        validarAbierto(ciclo);
        ciclo.setTotalIngresos(req.totalIngresos());
        recalcularSaldo(ciclo);
        return cicloRepo.save(ciclo);
    }

    @Transactional(readOnly = true)
    public CicloResumenResponse obtenerResumen(Long cicloId) {
        CicloMensual ciclo = obtenerPorId(cicloId);

        List<CicloResumenResponse.GastoFijoItemResponse> fijos = resolverGastosFijos(ciclo);
        List<GastoVariableResponse> variables = gastoVariableRepo
                .findByCicloMensualOrderByFechaDesc(ciclo).stream()
                .map(g -> new GastoVariableResponse(g.getId(), g.getCategoria().getNombre(),
                        g.getConcepto(), g.getMonto(), g.getFecha()))
                .toList();
        List<CuotaImputadaResponse> cuotas = cuotaRepo
                .findByAnioImpactoAndMesImpacto(ciclo.getAnio(), ciclo.getMes()).stream()
                .map(this::mapCuota)
                .toList();
        List<CicloResumenResponse.GastoEsperadoItemResponse> gastosEsperados = resolverGastosEsperados(ciclo);
        List<EventoGastoResponse> eventos = resolverEventos(ciclo);

        // Calcular totales en vivo — los campos del ciclo son cache eventual, no fuente de verdad
        BigDecimal totalFijos = fijos.stream()
                .map(CicloResumenResponse.GastoFijoItemResponse::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVariables = variables.stream()
                .map(GastoVariableResponse::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(totalEventos(eventos));
        BigDecimal totalCuotas = cuotas.stream()
                .map(CuotaImputadaResponse::montoEnPesos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoFinal = ciclo.getTotalIngresos()
                .subtract(totalFijos)
                .subtract(totalVariables)
                .subtract(totalCuotas);

        return new CicloResumenResponse(
                ciclo.getId(), ciclo.getAnio(), ciclo.getMes(), ciclo.getEstado(),
                ciclo.getFechaCierreReal(),
                ciclo.getTotalIngresos(), totalFijos, totalVariables,
                totalCuotas, saldoFinal,
                fijos, variables, cuotas, gastosEsperados, eventos);
    }

    /**
     * Calculadoras de tipo AHORRO con un objetivo en USD cargado ("Guardar") representan un
     * gasto esperado/proyectado para el ciclo abierto: todavía no es un GastoVariable real
     * (eso ocurre recién al "Efectivizar"), así que no se computa en ningún total — el
     * equivalente en pesos lo recalcula el cliente con la cotización vigente.
     */
    private List<CicloResumenResponse.GastoEsperadoItemResponse> resolverGastosEsperados(CicloMensual ciclo) {
        if (ciclo.getEstado() != EstadoCiclo.ABIERTO) return List.of();
        return calculadoraRepo.findByActivaTrueOrderByNombreAsc().stream()
                .filter(c -> c.getTipo() == TipoCalculadora.AHORRO && c.getAhorro() != null)
                .filter(c -> c.getAhorro().getObjetivoUsd().compareTo(BigDecimal.ZERO) > 0)
                .map(c -> new CicloResumenResponse.GastoEsperadoItemResponse(
                        c.getId(), c.getNombre(), c.getAhorro().getObjetivoUsd()))
                .toList();
    }

    @Transactional
    public CicloResumenResponse reabrirCiclo(Long cicloId) {
        CicloMensual ciclo = obtenerPorId(cicloId);
        if (ciclo.getEstado() == EstadoCiclo.ABIERTO) {
            throw new IllegalStateException("El ciclo ya está abierto");
        }
        // Eliminar snapshots del ciclo (se recalcularán en vivo)
        snapshotRepo.findByCicloMensual(ciclo).forEach(snapshotRepo::delete);
        ciclo.setEstado(EstadoCiclo.ABIERTO);
        ciclo.setFechaCierreReal(null);
        cicloRepo.save(ciclo);
        // Recalcular con datos en vivo
        recalcularTotales(cicloId);
        return obtenerResumen(cicloId);
    }

    @Transactional
    public CicloResumenResponse cerrarCiclo(Long cicloId) {
        CicloMensual ciclo = obtenerPorId(cicloId);

        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            throw new IllegalStateException("El ciclo %d/%d ya está cerrado".formatted(ciclo.getMes(), ciclo.getAnio()));
        }

        // 1. Obtener el resumen en vivo antes de sellar (para calcular totales finales)
        CicloResumenResponse resumen = obtenerResumen(cicloId);

        // 2. Crear snapshots inmutables de los gastos fijos activos en este momento
        gastoFijoRepo.findByActivoTrue().forEach(gastoFijo ->
                snapshotRepo.save(GastoFijoSnapshot.builder()
                        .cicloMensual(ciclo)
                        .gastoFijo(gastoFijo)
                        .montoAplicado(gastoFijo.getMontoActual())
                        .build())
        );

        // 3. Persistir los totales calculados y sellar el ciclo
        ciclo.setTotalFijos(resumen.totalFijos());
        ciclo.setTotalVariables(resumen.totalVariables());
        ciclo.setTotalCuotas(resumen.totalCuotas());
        ciclo.setSaldoFinal(resumen.saldoFinal());
        ciclo.setEstado(EstadoCiclo.CERRADO);
        ciclo.setFechaCierreReal(LocalDateTime.now());
        cicloRepo.save(ciclo);

        // 4. Devolver el resumen final ya usando los snapshots
        return obtenerResumen(cicloId);
    }

    // Llamado por otros servicios cada vez que cambia algo que afecta los totales
    @Transactional
    public void recalcularTotales(Long cicloId) {
        CicloMensual ciclo = obtenerPorId(cicloId);

        BigDecimal totalFijos = resolverTotalFijos(ciclo);
        BigDecimal totalVariables = gastoVariableRepo.findByCicloMensual(ciclo).stream()
                .map(GastoVariable::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(totalEventos(resolverEventos(ciclo)));
        BigDecimal totalCuotas = cuotaRepo
                .findByAnioImpactoAndMesImpacto(ciclo.getAnio(), ciclo.getMes()).stream()
                .map(CuotaImputada::getMontoEnPesos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ciclo.setTotalFijos(totalFijos);
        ciclo.setTotalVariables(totalVariables);
        ciclo.setTotalCuotas(totalCuotas);
        recalcularSaldo(ciclo);
        cicloRepo.save(ciclo);
    }

    public CicloMensual obtenerPorId(Long id) {
        return cicloRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ciclo no encontrado: " + id));
    }

    public CicloMensual obtenerPorAnioMes(Integer anio, Integer mes) {
        return cicloRepo.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new EntityNotFoundException("Ciclo no encontrado: %d/%d".formatted(mes, anio)));
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    public void validarAbierto(CicloMensual ciclo) {
        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            throw new IllegalStateException(
                    "El ciclo %d/%d está cerrado y no puede modificarse".formatted(ciclo.getMes(), ciclo.getAnio()));
        }
    }

    private void recalcularSaldo(CicloMensual ciclo) {
        BigDecimal saldo = ciclo.getTotalIngresos()
                .subtract(ciclo.getTotalFijos())
                .subtract(ciclo.getTotalVariables())
                .subtract(ciclo.getTotalCuotas());
        ciclo.setSaldoFinal(saldo);
    }

    private BigDecimal resolverTotalFijos(CicloMensual ciclo) {
        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            return snapshotRepo.findByCicloMensual(ciclo).stream()
                    .map(GastoFijoSnapshot::getMontoAplicado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return gastoFijoRepo.findByActivoTrue().stream()
                .map(GastoFijo::getMontoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CicloResumenResponse.GastoFijoItemResponse> resolverGastosFijos(CicloMensual ciclo) {
        if (ciclo.getEstado() == EstadoCiclo.CERRADO) {
            return snapshotRepo.findByCicloMensual(ciclo).stream()
                    .map(s -> new CicloResumenResponse.GastoFijoItemResponse(
                            s.getGastoFijo().getId(), s.getGastoFijo().getNombre(), s.getMontoAplicado()))
                    .toList();
        }
        return gastoFijoRepo.findByActivoTrue().stream()
                .map(f -> new CicloResumenResponse.GastoFijoItemResponse(f.getId(), f.getNombre(), f.getMontoActual()))
                .toList();
    }

    private List<EventoGastoResponse> resolverEventos(CicloMensual ciclo) {
        return eventoGastoRepo.findByCicloMensualOrderByIdDesc(ciclo).stream()
                .map(evento -> {
                    List<EventoGastoResponse.ItemResponse> items = evento.getItems().stream()
                            .map(i -> new EventoGastoResponse.ItemResponse(i.getId(), i.getConcepto(), i.getMonto()))
                            .toList();
                    BigDecimal total = evento.getItems().stream()
                            .map(EventoGastoItem::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new EventoGastoResponse(evento.getId(), evento.getNombre(), evento.getFecha(), total, items);
                })
                .toList();
    }

    private BigDecimal totalEventos(List<EventoGastoResponse> eventos) {
        return eventos.stream()
                .map(EventoGastoResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CuotaImputadaResponse mapCuota(CuotaImputada c) {
        return new CuotaImputadaResponse(
                c.getId(),
                c.getCompra().getId(),
                c.getCompra().getTarjeta().getId(),
                c.getCompra().getConcepto(),
                c.getCompra().getTarjeta().getNombre(),
                c.getNumeroCuota(),
                c.getCompra().getCantidadCuotas(),
                c.getMontoEnPesos(),
                c.getMesImpacto(),
                c.getAnioImpacto(),
                c.getEstado());
    }
}

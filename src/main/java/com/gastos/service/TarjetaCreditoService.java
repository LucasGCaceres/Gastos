package com.gastos.service;

import com.gastos.domain.model.CierreTarjetaMes;
import com.gastos.domain.model.CuotaImputada;
import com.gastos.domain.model.TarjetaCredito;
import com.gastos.domain.repository.CierreTarjetaMesRepository;
import com.gastos.domain.repository.CuotaImputadaRepository;
import com.gastos.domain.repository.TarjetaCreditoRepository;
import com.gastos.dto.request.CrearTarjetaRequest;
import com.gastos.dto.request.SetCierreTarjetaMesRequest;
import com.gastos.dto.response.CierreTarjetaMesResponse;
import com.gastos.dto.response.TarjetaDeudaResponse;
import com.gastos.dto.response.TarjetaResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarjetaCreditoService {

    private final TarjetaCreditoRepository tarjetaRepo;
    private final CierreTarjetaMesRepository cierreRepo;
    private final CuotaImputadaRepository cuotaRepo;

    @Transactional
    public TarjetaResponse crear(CrearTarjetaRequest req) {
        TarjetaCredito tarjeta = TarjetaCredito.builder()
                .nombre(req.nombre())
                .banco(req.banco())
                .diaCierreEstimado(req.diaCierreEstimado())
                .diaVencimientoEstimado(req.diaVencimientoEstimado())
                .build();
        return toResponse(tarjetaRepo.save(tarjeta));
    }

    @Transactional
    public TarjetaResponse editar(Long id, CrearTarjetaRequest req) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + id));
        tarjeta.setNombre(req.nombre());
        tarjeta.setBanco(req.banco());
        tarjeta.setDiaCierreEstimado(req.diaCierreEstimado());
        tarjeta.setDiaVencimientoEstimado(req.diaVencimientoEstimado());
        return toResponse(tarjetaRepo.save(tarjeta));
    }

    @Transactional(readOnly = true)
    public List<TarjetaResponse> listarActivas() {
        return tarjetaRepo.findByActivaTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TarjetaResponse> listarTodas() {
        return tarjetaRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CierreTarjetaMesResponse setCierreMes(Long tarjetaId, SetCierreTarjetaMesRequest req) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(tarjetaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + tarjetaId));
        CierreTarjetaMes cierre = cierreRepo
                .findByTarjetaAndAnioAndMes(tarjeta, req.anio(), req.mes())
                .orElseGet(() -> CierreTarjetaMes.builder().tarjeta(tarjeta).anio(req.anio()).mes(req.mes()).build());
        cierre.setFechaCierreReal(req.fechaCierreReal());
        cierre.setFechaVencimientoReal(req.fechaVencimientoReal());
        return toCierreResponse(cierreRepo.save(cierre));
    }

    @Transactional(readOnly = true)
    public CierreTarjetaMesResponse getCierreMes(Long tarjetaId, Integer anio, Integer mes) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(tarjetaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + tarjetaId));
        return cierreRepo.findByTarjetaAndAnioAndMes(tarjeta, anio, mes)
                .map(this::toCierreResponse)
                .orElse(null);
    }

    @Transactional
    public void desactivar(Long id) {
        actualizarEstado(id, false);
    }

    @Transactional
    public TarjetaResponse actualizarEstado(Long id, boolean activa) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + id));
        tarjeta.setActiva(activa);
        return toResponse(tarjetaRepo.save(tarjeta));
    }

    /**
     * Desglosa cuánto se le debe a cada tarjeta/entidad activa en un mes/año dado,
     * sumando las cuotas imputadas que caen en ese ciclo.
     */
    @Transactional(readOnly = true)
    public List<TarjetaDeudaResponse> desgloseDeudas(Integer anio, Integer mes) {
        List<CuotaImputada> cuotas = cuotaRepo.findByAnioImpactoAndMesImpacto(anio, mes);
        Map<Long, BigDecimal> totalesPorTarjeta = cuotas.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCompra().getTarjeta().getId(),
                        Collectors.reducing(BigDecimal.ZERO, CuotaImputada::getMontoEnPesos, BigDecimal::add)));

        return tarjetaRepo.findByActivaTrue().stream()
                .map(t -> new TarjetaDeudaResponse(t.getId(), t.getNombre(), t.getBanco(),
                        totalesPorTarjeta.getOrDefault(t.getId(), BigDecimal.ZERO)))
                .toList();
    }

    private TarjetaResponse toResponse(TarjetaCredito t) {
        return new TarjetaResponse(t.getId(), t.getNombre(), t.getBanco(),
                t.getDiaCierreEstimado(), t.getDiaVencimientoEstimado(), t.getActiva());
    }

    private CierreTarjetaMesResponse toCierreResponse(CierreTarjetaMes c) {
        return new CierreTarjetaMesResponse(c.getId(), c.getTarjeta().getId(),
                c.getAnio(), c.getMes(), c.getFechaCierreReal(), c.getFechaVencimientoReal());
    }
}

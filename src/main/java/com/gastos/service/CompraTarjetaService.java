package com.gastos.service;

import com.gastos.domain.enums.EstadoCuota;
import com.gastos.domain.enums.Moneda;
import com.gastos.domain.model.CompraTarjeta;
import com.gastos.domain.model.CuotaImputada;
import com.gastos.domain.model.TarjetaCredito;
import com.gastos.domain.repository.CompraTarjetaRepository;
import com.gastos.domain.repository.CicloMensualRepository;
import com.gastos.domain.repository.CuotaImputadaRepository;
import com.gastos.domain.repository.TarjetaCreditoRepository;
import com.gastos.dto.request.RegistrarCompraRequest;
import com.gastos.dto.response.CompraTarjetaResponse;
import com.gastos.dto.response.CuotaImputadaResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompraTarjetaService {

    private final CompraTarjetaRepository compraRepo;
    private final TarjetaCreditoRepository tarjetaRepo;
    private final CicloMensualRepository cicloRepo;
    private final CuotaImputadaRepository cuotaRepo;
    private final CuotaGeneratorService cuotaGenerator;
    private final CicloMensualService cicloService;

    @Transactional
    public CompraTarjetaResponse registrarCompra(RegistrarCompraRequest req) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(req.tarjetaId())
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + req.tarjetaId()));

        BigDecimal cotizacion = resolverCotizacion(req);
        BigDecimal montoEnPesos = req.montoOriginal().multiply(cotizacion);

        int yaAbonadas = req.cuotasYaAbonadas() != null ? req.cuotasYaAbonadas() : 0;
        if (yaAbonadas > req.cantidadCuotas()) {
            throw new IllegalArgumentException("Las cuotas ya abonadas no pueden superar el total de cuotas");
        }

        CompraTarjeta compra = CompraTarjeta.builder()
                .tarjeta(tarjeta)
                .concepto(req.concepto())
                .fechaCompra(req.fechaCompra())
                .monedaOriginal(req.monedaOriginal())
                .cotizacionAplicada(cotizacion)
                .montoOriginal(req.montoOriginal())
                .montoEnPesos(montoEnPesos)
                .cantidadCuotas(req.cantidadCuotas())
                .cuotasYaAbonadas(yaAbonadas)
                .build();

        cuotaGenerator.validarCoherenciaCuotasAbonadas(compra);
        List<CuotaImputada> cuotas = cuotaGenerator.generarCuotas(compra);
        compra.getCuotas().addAll(cuotas);

        CompraTarjeta saved = compraRepo.save(compra);

        // Recalcular totales en cada ciclo que recibe una cuota, para que el saldo proyectado sea visible
        cuotas.stream()
                .collect(Collectors.groupingBy(c -> c.getAnioImpacto() * 100 + c.getMesImpacto()))
                .forEach((key, grupo) -> {
                    CuotaImputada c = grupo.get(0);
                    cicloRepo.findByAnioAndMes(c.getAnioImpacto(), c.getMesImpacto())
                            .ifPresent(ciclo -> cicloService.recalcularTotales(ciclo.getId()));
                });

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompraTarjetaResponse obtener(Long compraId) {
        return toResponse(compraRepo.findById(compraId)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada: " + compraId)));
    }

    @Transactional
    public CompraTarjetaResponse editarCompra(Long compraId, RegistrarCompraRequest req) {
        CompraTarjeta compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada: " + compraId));

        // Ciclos que actualmente tienen cuotas de esta compra
        List<int[]> ciclosViejos = compra.getCuotas().stream()
                .map(c -> new int[]{c.getAnioImpacto(), c.getMesImpacto()})
                .distinct().toList();

        TarjetaCredito tarjeta = tarjetaRepo.findById(req.tarjetaId())
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + req.tarjetaId()));

        int yaAbonadas = req.cuotasYaAbonadas() != null ? req.cuotasYaAbonadas() : 0;
        if (yaAbonadas > req.cantidadCuotas()) {
            throw new IllegalArgumentException("Las cuotas ya abonadas no pueden superar el total de cuotas");
        }

        BigDecimal cotizacion = resolverCotizacion(req);
        BigDecimal montoEnPesos = req.montoOriginal().multiply(cotizacion);

        // Actualizar campos — orphanRemoval borra las cuotas viejas al hacer clear
        compra.getCuotas().clear();
        compra.setTarjeta(tarjeta);
        compra.setConcepto(req.concepto());
        compra.setFechaCompra(req.fechaCompra());
        compra.setMonedaOriginal(req.monedaOriginal());
        compra.setCotizacionAplicada(cotizacion);
        compra.setMontoOriginal(req.montoOriginal());
        compra.setMontoEnPesos(montoEnPesos);
        compra.setCantidadCuotas(req.cantidadCuotas());
        compra.setCuotasYaAbonadas(yaAbonadas);

        cuotaGenerator.validarCoherenciaCuotasAbonadas(compra);
        List<CuotaImputada> nuevasCuotas = cuotaGenerator.generarCuotas(compra);
        compra.getCuotas().addAll(nuevasCuotas);
        CompraTarjeta saved = compraRepo.save(compra);

        // Recalcular ciclos viejos + nuevos
        ciclosViejos.forEach(am ->
                cicloRepo.findByAnioAndMes(am[0], am[1])
                        .ifPresent(ciclo -> cicloService.recalcularTotales(ciclo.getId())));
        nuevasCuotas.stream()
                .collect(Collectors.groupingBy(c -> c.getAnioImpacto() * 100 + c.getMesImpacto()))
                .forEach((key, grupo) -> {
                    CuotaImputada c = grupo.get(0);
                    cicloRepo.findByAnioAndMes(c.getAnioImpacto(), c.getMesImpacto())
                            .ifPresent(ciclo -> cicloService.recalcularTotales(ciclo.getId()));
                });

        return toResponse(saved);
    }

    @Transactional
    public void eliminarCompra(Long compraId) {
        CompraTarjeta compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada: " + compraId));

        // Recolectar ciclos afectados antes de borrar
        List<int[]> ciclosAfectados = compra.getCuotas().stream()
                .map(c -> new int[]{c.getAnioImpacto(), c.getMesImpacto()})
                .distinct()
                .toList();

        compraRepo.delete(compra); // cascade elimina cuotas

        // Recalcular saldo de ciclos que tenían cuotas de esta compra
        ciclosAfectados.forEach(am ->
                cicloRepo.findByAnioAndMes(am[0], am[1])
                        .ifPresent(ciclo -> cicloService.recalcularTotales(ciclo.getId())));
    }

    @Transactional
    public CuotaImputadaResponse actualizarEstadoCuota(Long cuotaId, EstadoCuota estado) {
        CuotaImputada cuota = cuotaRepo.findById(cuotaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuota no encontrada: " + cuotaId));
        cuota.setEstado(estado);
        CuotaImputada saved = cuotaRepo.save(cuota);
        CompraTarjeta compra = saved.getCompra();
        return new CuotaImputadaResponse(
                saved.getId(), compra.getId(), compra.getTarjeta().getId(),
                compra.getConcepto(), compra.getTarjeta().getNombre(),
                saved.getNumeroCuota(), compra.getCantidadCuotas(),
                saved.getMontoEnPesos(), saved.getMesImpacto(), saved.getAnioImpacto(), saved.getEstado());
    }

    @Transactional(readOnly = true)
    public List<CompraTarjetaResponse> listarPorTarjeta(Long tarjetaId) {
        TarjetaCredito tarjeta = tarjetaRepo.findById(tarjetaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada: " + tarjetaId));
        return compraRepo.findByTarjeta(tarjeta).stream().map(this::toResponse).toList();
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private BigDecimal resolverCotizacion(RegistrarCompraRequest req) {
        if (req.monedaOriginal() == Moneda.USD) {
            if (req.cotizacionAplicada() == null || req.cotizacionAplicada().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Se requiere cotizacion_aplicada > 0 para compras en USD");
            }
            return req.cotizacionAplicada();
        }
        return BigDecimal.ONE;
    }

    private CompraTarjetaResponse toResponse(CompraTarjeta c) {
        List<CuotaImputadaResponse> cuotas = c.getCuotas().stream()
                .map(q -> new CuotaImputadaResponse(
                        q.getId(), c.getId(), c.getTarjeta().getId(),
                        c.getConcepto(), c.getTarjeta().getNombre(),
                        q.getNumeroCuota(), c.getCantidadCuotas(),
                        q.getMontoEnPesos(), q.getMesImpacto(), q.getAnioImpacto(), q.getEstado()))
                .toList();

        return new CompraTarjetaResponse(
                c.getId(), c.getTarjeta().getId(), c.getTarjeta().getNombre(),
                c.getConcepto(), c.getFechaCompra(), c.getMonedaOriginal(),
                c.getMontoOriginal(), c.getCotizacionAplicada(),
                c.getMontoEnPesos(), c.getCantidadCuotas(), c.getCuotasYaAbonadas(), cuotas);
    }
}

package com.gastos.service;

import com.gastos.domain.enums.TipoMovimientoInversion;
import com.gastos.domain.model.InstrumentoInversion;
import com.gastos.domain.model.MovimientoInversion;
import com.gastos.domain.repository.InstrumentoInversionRepository;
import com.gastos.domain.repository.MovimientoInversionRepository;
import com.gastos.dto.request.RegistrarMovimientoRequest;
import com.gastos.dto.response.MovimientoInversionResponse;
import com.gastos.dto.response.PatrimonioResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InversionService {

    private final MovimientoInversionRepository movimientoRepo;
    private final InstrumentoInversionRepository instrumentoRepo;
    private final CicloMensualService cicloService;

    @Transactional
    public MovimientoInversionResponse registrar(RegistrarMovimientoRequest req) {
        InstrumentoInversion instrumento = instrumentoRepo.findById(req.instrumentoId())
                .orElseThrow(() -> new EntityNotFoundException("Instrumento no encontrado: " + req.instrumentoId()));
        var ciclo = cicloService.obtenerPorId(req.cicloMensualId());

        BigDecimal montoTotal = req.cantidad().multiply(req.precioUnitario()).setScale(2, RoundingMode.HALF_UP);

        MovimientoInversion mov = MovimientoInversion.builder()
                .instrumento(instrumento)
                .cicloMensual(ciclo)
                .tipo(req.tipo())
                .fecha(req.fecha())
                .cantidad(req.cantidad())
                .precioUnitario(req.precioUnitario())
                .montoTotalPesos(montoTotal)
                .build();

        return toResponse(movimientoRepo.save(mov));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInversionResponse> listarPorCiclo(Long cicloId) {
        var ciclo = cicloService.obtenerPorId(cicloId);
        return movimientoRepo.findByCicloMensual(ciclo).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatrimonioResponse calcularPatrimonio() {
        List<InstrumentoInversion> instrumentos = instrumentoRepo.findByActivoTrue();
        List<PatrimonioResponse.PosicionResponse> posiciones = new ArrayList<>();

        for (InstrumentoInversion instrumento : instrumentos) {
            List<MovimientoInversion> movimientos = movimientoRepo.findByInstrumento(instrumento);
            if (movimientos.isEmpty()) continue;

            // Calcular cantidad neta y costo promedio ponderado
            BigDecimal cantidadComprada = movimientos.stream()
                    .filter(m -> m.getTipo() == TipoMovimientoInversion.COMPRA)
                    .map(MovimientoInversion::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cantidadVendida = movimientos.stream()
                    .filter(m -> m.getTipo() == TipoMovimientoInversion.VENTA)
                    .map(MovimientoInversion::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cantidadNeta = cantidadComprada.subtract(cantidadVendida);

            if (cantidadNeta.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal costoTotalCompras = movimientos.stream()
                    .filter(m -> m.getTipo() == TipoMovimientoInversion.COMPRA)
                    .map(MovimientoInversion::getMontoTotalPesos)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costoPromedio = cantidadComprada.compareTo(BigDecimal.ZERO) > 0
                    ? costoTotalCompras.divide(cantidadComprada, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal costoTotal = cantidadNeta.multiply(costoPromedio).setScale(2, RoundingMode.HALF_UP);

            posiciones.add(new PatrimonioResponse.PosicionResponse(
                    instrumento.getId(),
                    instrumento.getNombre(),
                    instrumento.getTicker(),
                    instrumento.getTipo(),
                    cantidadNeta,
                    costoPromedio,
                    costoTotal
            ));
        }

        BigDecimal totalInvertido = posiciones.stream()
                .map(PatrimonioResponse.PosicionResponse::costoTotalPesos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PatrimonioResponse(totalInvertido, posiciones);
    }

    private MovimientoInversionResponse toResponse(MovimientoInversion m) {
        return new MovimientoInversionResponse(
                m.getId(),
                m.getInstrumento().getId(),
                m.getInstrumento().getNombre(),
                m.getInstrumento().getTipo().name(),
                m.getTipo(),
                m.getFecha(),
                m.getCantidad(),
                m.getPrecioUnitario(),
                m.getMontoTotalPesos()
        );
    }
}

package com.gastos.service;

import com.gastos.domain.enums.EstadoCiclo;
import com.gastos.domain.enums.TipoCalculadora;
import com.gastos.domain.model.*;
import com.gastos.domain.repository.*;
import com.gastos.dto.request.*;
import com.gastos.dto.response.CalculadoraResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class CalculadoraService {

    private static final BigDecimal FACTOR_PRIMERO = BigDecimal.ONE;
    private static final BigDecimal FACTOR_SEGUNDO = new BigDecimal("0.50");
    private static final BigDecimal FACTOR_TERCERO = new BigDecimal("0.25");

    private final CalculadoraRepository calculadoraRepo;
    private final CalculadoraAhorroRepository ahorroRepo;
    private final AhorroOperacionRepository operacionRepo;
    private final GastoFijoRepository gastoFijoRepo;
    private final GastoFijoService gastoFijoService;
    private final GastoVariableRepository gastoVariableRepo;
    private final CategoriaRepository categoriaRepo;
    private final CicloMensualRepository cicloRepo;

    @Transactional(readOnly = true)
    public List<CalculadoraResponse> listar() {
        return calculadoraRepo.findByActivaTrueOrderByNombreAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CalculadoraResponse obtener(Long id) {
        return toResponse(get(id));
    }

    @Transactional
    public CalculadoraResponse crear(CrearCalculadoraRequest req) {
        GastoFijo gastoFijo = resolverGastoFijo(req);

        Calculadora calc = Calculadora.builder()
                .nombre(req.nombre())
                .tipo(req.tipo())
                .gastoFijo(gastoFijo)
                .build();
        calculadoraRepo.save(calc);

        if (req.tipo() == TipoCalculadora.AHORRO) {
            CalculadoraAhorro ahorro = CalculadoraAhorro.builder()
                    .calculadora(calc)
                    .objetivoUsd(BigDecimal.ZERO)
                    .build();
            calc.setAhorro(ahorroRepo.save(ahorro));
        }

        return toResponse(calculadoraRepo.save(calc));
    }

    @Transactional
    public CalculadoraResponse actualizarItems(Long id, ActualizarCalculadoraItemsRequest req) {
        Calculadora calc = get(id);
        calc.getItems().clear();
        AtomicInteger orden = new AtomicInteger(0);
        req.items().forEach(dto -> calc.getItems().add(
                CalculadoraItem.builder()
                        .calculadora(calc)
                        .nombre(dto.nombre())
                        .cantidad(dto.cantidad())
                        .precioUnitario(dto.precioUnitario())
                        .orden(orden.getAndIncrement())
                        .build()));
        return toResponse(calculadoraRepo.save(calc));
    }

    @Transactional
    public CalculadoraResponse actualizarRutas(Long id, ActualizarCalculadoraRutasRequest req) {
        Calculadora calc = get(id);
        calc.getRutas().clear();
        req.rutas().forEach(rutaDto -> {
            CalculadoraRuta ruta = CalculadoraRuta.builder()
                    .calculadora(calc)
                    .nombre(rutaDto.nombre())
                    .viajesPorMes(rutaDto.viajesPorMes())
                    .build();
            AtomicInteger orden = new AtomicInteger(0);
            rutaDto.tramos().forEach(tramoDto -> ruta.getTramos().add(
                    CalculadoraTramo.builder()
                            .ruta(ruta)
                            .nombre(tramoDto.nombre())
                            .precio(tramoDto.precio())
                            .orden(orden.getAndIncrement())
                            .build()));
            calc.getRutas().add(ruta);
        });
        return toResponse(calculadoraRepo.save(calc));
    }

    @Transactional
    public CalculadoraResponse actualizarAhorro(Long id, ActualizarCalculadoraAhorroRequest req) {
        Calculadora calc = get(id);
        if (calc.getTipo() != TipoCalculadora.AHORRO) {
            throw new IllegalStateException("Esta calculadora no es de tipo AHORRO");
        }
        CalculadoraAhorro ahorro = calc.getAhorro();
        if (ahorro == null) {
            ahorro = CalculadoraAhorro.builder().calculadora(calc).build();
            calc.setAhorro(ahorro);
        }
        ahorro.setObjetivoUsd(req.objetivoUsd());
        ahorroRepo.save(ahorro);
        calculadoraRepo.save(calc);
        return toResponse(get(id));
    }

    @Transactional
    public CalculadoraResponse efectivizarAhorro(Long id, EfectivizarAhorroRequest req) {
        Calculadora calc = get(id);
        if (calc.getTipo() != TipoCalculadora.AHORRO) {
            throw new IllegalStateException("Esta calculadora no es de tipo AHORRO");
        }
        CalculadoraAhorro ahorro = calc.getAhorro();
        if (ahorro == null || ahorro.getObjetivoUsd().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El objetivo en USD debe ser mayor a cero antes de efectivizar");
        }

        BigDecimal montoArs = ahorro.getObjetivoUsd()
                .multiply(req.cotizacionActual())
                .setScale(2, RoundingMode.HALF_UP);

        // Buscar ciclo abierto
        CicloMensual cicloAbierto = cicloRepo.findByEstadoOrderByAnioDescMesDesc(EstadoCiclo.ABIERTO)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ciclo abierto para imputar el gasto"));

        // Buscar categoría "Ahorro / Inversiones", fallback a "Otros"
        Categoria categoria = categoriaRepo.findAll().stream()
                .filter(c -> c.getNombre().toLowerCase().contains("ahorro") ||
                             c.getNombre().toLowerCase().contains("invers"))
                .findFirst()
                .orElseGet(() -> categoriaRepo.findAll().stream()
                        .filter(c -> c.getNombre().equalsIgnoreCase("Otros"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No hay categorías disponibles")));

        String concepto = (req.nombreGasto() != null && !req.nombreGasto().isBlank())
                ? req.nombreGasto()
                : "Compra USD %s @ $%s".formatted(ahorro.getObjetivoUsd(), req.cotizacionActual());

        // Registrar gasto variable
        gastoVariableRepo.save(GastoVariable.builder()
                .cicloMensual(cicloAbierto)
                .categoria(categoria)
                .concepto(concepto)
                .monto(montoArs)
                .fecha(LocalDate.now())
                .build());

        // Guardar en historial
        AhorroOperacion operacion = AhorroOperacion.builder()
                .ahorro(ahorro)
                .objetivoUsd(ahorro.getObjetivoUsd())
                .cotizacion(req.cotizacionActual())
                .montoArs(montoArs)
                .concepto(concepto)
                .fecha(LocalDate.now())
                .mesImpacto(cicloAbierto.getMes())
                .anioImpacto(cicloAbierto.getAnio())
                .build();
        operacionRepo.save(operacion);

        // Resetear objetivo para nueva operación
        ahorro.setObjetivoUsd(BigDecimal.ZERO);
        ahorroRepo.save(ahorro);

        return toResponse(get(id));
    }

    @Transactional
    public CalculadoraResponse aplicar(Long id) {
        Calculadora calc = get(id);
        if (calc.getGastoFijo() == null) {
            throw new IllegalStateException("Esta calculadora no tiene un gasto fijo vinculado");
        }
        BigDecimal total = calcularTotal(calc);
        // Si el gasto fijo vinculado había quedado desactivado, reactivarlo: de lo contrario
        // el total recalculado se guarda pero queda invisible en los ciclos (findByActivoTrue lo excluye).
        gastoFijoService.activar(calc.getGastoFijo().getId());
        gastoFijoService.editar(calc.getGastoFijo().getId(),
                new CrearGastoFijoRequest(calc.getGastoFijo().getNombre(), total));
        return toResponse(get(id));
    }

    @Transactional
    public void desactivar(Long id) {
        Calculadora calc = get(id);
        calc.setActiva(false);
        calculadoraRepo.save(calc);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private Calculadora get(Long id) {
        return calculadoraRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Calculadora no encontrada: " + id));
    }

    private GastoFijo resolverGastoFijo(CrearCalculadoraRequest req) {
        if (req.gastoFijoId() != null) {
            return gastoFijoRepo.findById(req.gastoFijoId())
                    .orElseThrow(() -> new EntityNotFoundException("Gasto fijo no encontrado: " + req.gastoFijoId()));
        }
        if (req.nuevoGastoFijoNombre() != null && !req.nuevoGastoFijoNombre().isBlank()) {
            return gastoFijoRepo.save(GastoFijo.builder()
                    .nombre(req.nuevoGastoFijoNombre())
                    .montoActual(BigDecimal.ZERO)
                    .activo(true)
                    .build());
        }
        return null;
    }

    private BigDecimal calcularTotal(Calculadora calc) {
        return switch (calc.getTipo()) {
            case GENERICA -> calc.getItems().stream()
                    .map(i -> i.getCantidad().multiply(i.getPrecioUnitario()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            case SUBE -> calc.getRutas().stream()
                    .map(this::costoMensualRuta)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            case AHORRO -> BigDecimal.ZERO;
        };
    }

    private BigDecimal costoMensualRuta(CalculadoraRuta ruta) {
        BigDecimal costoPorViaje = ruta.getTramos().stream()
                .map(t -> t.getPrecio().multiply(factorSube(t.getOrden())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return costoPorViaje.multiply(BigDecimal.valueOf(ruta.getViajesPorMes()));
    }

    private BigDecimal factorSube(int orden) {
        if (orden == 0) return FACTOR_PRIMERO;
        if (orden == 1) return FACTOR_SEGUNDO;
        return FACTOR_TERCERO;
    }

    private CalculadoraResponse toResponse(Calculadora calc) {
        BigDecimal total = calcularTotal(calc);

        List<CalculadoraResponse.ItemResponse> items = calc.getItems().stream()
                .map(i -> new CalculadoraResponse.ItemResponse(
                        i.getId(), i.getNombre(), i.getCantidad(), i.getPrecioUnitario(),
                        i.getCantidad().multiply(i.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP)))
                .toList();

        List<CalculadoraResponse.RutaResponse> rutas = calc.getRutas().stream()
                .map(r -> {
                    List<CalculadoraResponse.TramoResponse> tramos = r.getTramos().stream()
                            .map(t -> {
                                BigDecimal factor = factorSube(t.getOrden());
                                return new CalculadoraResponse.TramoResponse(
                                        t.getId(), t.getNombre(), t.getPrecio(),
                                        t.getOrden(), factor,
                                        t.getPrecio().multiply(factor).setScale(2, RoundingMode.HALF_UP));
                            }).toList();
                    return new CalculadoraResponse.RutaResponse(
                            r.getId(), r.getNombre(), r.getViajesPorMes(),
                            tramos, costoMensualRuta(r).setScale(2, RoundingMode.HALF_UP));
                }).toList();

        CalculadoraResponse.AhorroData ahorroData = null;
        if (calc.getTipo() == TipoCalculadora.AHORRO && calc.getAhorro() != null) {
            CalculadoraAhorro a = calc.getAhorro();
            List<CalculadoraResponse.OperacionData> operaciones = a.getOperaciones().stream()
                    .map(op -> new CalculadoraResponse.OperacionData(
                            op.getId(), op.getObjetivoUsd(), op.getCotizacion(),
                            op.getMontoArs(), op.getConcepto(), op.getFecha().toString(),
                            op.getMesImpacto(), op.getAnioImpacto()))
                    .toList();
            ahorroData = new CalculadoraResponse.AhorroData(a.getObjetivoUsd(), operaciones);
        }

        return new CalculadoraResponse(
                calc.getId(), calc.getNombre(), calc.getTipo(),
                calc.getGastoFijo() != null ? calc.getGastoFijo().getId() : null,
                calc.getGastoFijo() != null ? calc.getGastoFijo().getNombre() : null,
                total, items, rutas, ahorroData);
    }
}

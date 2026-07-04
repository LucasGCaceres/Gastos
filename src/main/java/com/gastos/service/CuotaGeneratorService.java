package com.gastos.service;

import com.gastos.domain.model.CierreTarjetaMes;
import com.gastos.domain.model.CompraTarjeta;
import com.gastos.domain.model.CuotaImputada;
import com.gastos.domain.model.TarjetaCredito;
import com.gastos.domain.repository.CierreTarjetaMesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CuotaGeneratorService {

    private final CierreTarjetaMesRepository cierreTarjetaMesRepository;

    /**
     * Genera la lista de cuotas proyectadas para una compra.
     * No persiste — el llamador es responsable de guardar junto a la CompraTarjeta.
     * Solo genera las cuotas pendientes (numeroCuota > cuotasYaAbonadas); si la compra
     * ya está totalmente abonada (cuotasYaAbonadas == cantidadCuotas) no genera ninguna,
     * evitando proyectar cuotas "fantasma" hacia ciclos futuros.
     */
    public List<CuotaImputada> generarCuotas(CompraTarjeta compra) {
        YearMonth mesPrimeraCuota = calcularMesPrimeraCuota(compra);
        int yaAbonadas = compra.getCuotasYaAbonadas() != null ? compra.getCuotasYaAbonadas() : 0;
        int nroCuotaInicio = yaAbonadas + 1;
        return distribuirCuotas(compra, mesPrimeraCuota, nroCuotaInicio);
    }

    /**
     * Valida que las cuotas ya abonadas sean coherentes con la fecha de compra: la última
     * cuota declarada como abonada no puede caer en un mes futuro respecto de hoy, porque
     * eso significaría que el usuario dice haber pagado algo que todavía no venció, dejando
     * un registro "fantasma" que aparecería de golpe en un ciclo futuro.
     */
    public void validarCoherenciaCuotasAbonadas(CompraTarjeta compra) {
        int yaAbonadas = compra.getCuotasYaAbonadas() != null ? compra.getCuotasYaAbonadas() : 0;
        if (yaAbonadas == 0) return;

        YearMonth mesPrimeraCuota = calcularMesPrimeraCuota(compra);
        YearMonth mesUltimaAbonada = mesPrimeraCuota.plusMonths(yaAbonadas - 1);

        if (mesUltimaAbonada.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException(
                    "Cuotas ya abonadas inconsistentes con la fecha de compra: la cuota "
                            + yaAbonadas + " recién vencería en " + mesUltimaAbonada
                            + ", todavía no pudo haber sido pagada");
        }
    }

    private YearMonth calcularMesPrimeraCuota(CompraTarjeta compra) {
        LocalDate fechaCompra = compra.getFechaCompra();
        TarjetaCredito tarjeta = compra.getTarjeta();

        LocalDate fechaCierre = resolverFechaCierre(tarjeta, fechaCompra.getYear(), fechaCompra.getMonthValue());

        // Regla de salto: si la compra es después del cierre, la cuota 1 cae el mes siguiente
        return fechaCompra.isAfter(fechaCierre)
                ? YearMonth.of(fechaCompra.getYear(), fechaCompra.getMonth()).plusMonths(1)
                : YearMonth.of(fechaCompra.getYear(), fechaCompra.getMonth());
    }

    /**
     * Resuelve la fecha de cierre para un mes dado.
     * Primero busca un override real en cierres_tarjeta_mes; si no existe, construye
     * la fecha usando el día estimado de la tarjeta, ajustando al último día del mes
     * cuando el estimado supera los días del mes (ej: día 31 en febrero).
     */
    private LocalDate resolverFechaCierre(TarjetaCredito tarjeta, int anio, int mes) {
        Optional<CierreTarjetaMes> override = cierreTarjetaMesRepository
                .findByTarjetaAndAnioAndMes(tarjeta, anio, mes);

        if (override.isPresent()) {
            return override.get().getFechaCierreReal();
        }

        // Fallback al día estimado, clampeado al último día real del mes
        int diaEstimado = tarjeta.getDiaCierreEstimado();
        int ultimoDia = YearMonth.of(anio, mes).lengthOfMonth();
        return LocalDate.of(anio, mes, Math.min(diaEstimado, ultimoDia));
    }

    /**
     * Distribuye el monto total en N cuotas iguales.
     * La última cuota absorbe el centavo de diferencia por redondeo.
     */
    /**
     * @param mesPrimeraCuota  mes donde cae la cuota número 1 (antes de descontar abonadas)
     * @param nroCuotaInicio  número de la primera cuota a generar (1 si no hay abonadas, 2 si ya abonó 1, etc.)
     */
    private List<CuotaImputada> distribuirCuotas(CompraTarjeta compra, YearMonth mesPrimeraCuota, int nroCuotaInicio) {
        int totalCuotas = compra.getCantidadCuotas();
        int cuotasAGenerar = totalCuotas - (nroCuotaInicio - 1);
        if (cuotasAGenerar <= 0) return new ArrayList<>();

        BigDecimal total = compra.getMontoEnPesos();
        // Monto por cuota distribuido equitativamente sobre el total de cuotas
        BigDecimal montoCuota = total.divide(BigDecimal.valueOf(totalCuotas), 2, RoundingMode.DOWN);
        BigDecimal montoUltima = total.subtract(montoCuota.multiply(BigDecimal.valueOf(totalCuotas - 1)));

        List<CuotaImputada> cuotas = new ArrayList<>(cuotasAGenerar);
        YearMonth mesActual = mesPrimeraCuota.plusMonths(nroCuotaInicio - 1);

        for (int i = nroCuotaInicio; i <= totalCuotas; i++) {
            BigDecimal monto = (i == totalCuotas) ? montoUltima : montoCuota;

            cuotas.add(CuotaImputada.builder()
                    .compra(compra)
                    .numeroCuota(i)
                    .montoEnPesos(monto)
                    .mesImpacto(mesActual.getMonthValue())
                    .anioImpacto(mesActual.getYear())
                    .build());

            mesActual = mesActual.plusMonths(1);
        }

        return cuotas;
    }
}

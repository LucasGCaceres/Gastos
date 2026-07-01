package com.gastos.service;

import com.gastos.domain.enums.Moneda;
import com.gastos.domain.model.CierreTarjetaMes;
import com.gastos.domain.model.CompraTarjeta;
import com.gastos.domain.model.CuotaImputada;
import com.gastos.domain.model.TarjetaCredito;
import com.gastos.domain.repository.CierreTarjetaMesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaGeneratorServiceTest {

    @Mock
    private CierreTarjetaMesRepository cierreTarjetaMesRepository;

    @InjectMocks
    private CuotaGeneratorService service;

    private TarjetaCredito tarjeta;

    @BeforeEach
    void setUp() {
        // Tarjeta con cierre estimado el día 20
        tarjeta = TarjetaCredito.builder()
                .id(1L)
                .nombre("Visa")
                .diaCierreEstimado(20)
                .diaVencimientoEstimado(10)
                .build();

        // Por defecto no hay override real de fechas
        when(cierreTarjetaMesRepository.findByTarjetaAndAnioAndMes(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    // ── Regla de salto ────────────────────────────────────────────────────────

    @Test
    void compraAntesDelCierre_primeraCuotaEnMesActual() {
        // Compra el 15 de junio, cierre el 20 → cuota 1 en junio
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 15), 1).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas).hasSize(1);
        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(6);
        assertThat(cuotas.get(0).getAnioImpacto()).isEqualTo(2026);
    }

    @Test
    void compraDespuesDelCierre_primeraCuotaEnMesSiguiente() {
        // Compra el 25 de junio, cierre el 20 → cuota 1 en julio
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 25), 1).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas).hasSize(1);
        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(7);
        assertThat(cuotas.get(0).getAnioImpacto()).isEqualTo(2026);
    }

    @Test
    void compraDiaExactoCierre_primeraCuotaEnMesActual() {
        // Compra el día 20 (mismo día del cierre) → no saltó, cae en junio
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 20), 1).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(6);
    }

    // ── Proyección de cuotas múltiples ───────────────────────────────────────

    @Test
    void tresCotas_seProyectanEnMesesConsecutivos() {
        // Compra el 10 de junio en 3 cuotas → cuotas en junio, julio, agosto
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 10), 3).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas).hasSize(3);
        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(6);
        assertThat(cuotas.get(1).getMesImpacto()).isEqualTo(7);
        assertThat(cuotas.get(2).getMesImpacto()).isEqualTo(8);
        assertThat(cuotas).allMatch(c -> c.getAnioImpacto() == 2026);
    }

    @Test
    void rolloverDiciembre_cuotasSaltanDeAnio() {
        // Compra el 10 de noviembre en 3 cuotas → nov, dic, ene del año siguiente
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 11, 10), 3).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(11);
        assertThat(cuotas.get(0).getAnioImpacto()).isEqualTo(2026);
        assertThat(cuotas.get(1).getMesImpacto()).isEqualTo(12);
        assertThat(cuotas.get(1).getAnioImpacto()).isEqualTo(2026);
        assertThat(cuotas.get(2).getMesImpacto()).isEqualTo(1);
        assertThat(cuotas.get(2).getAnioImpacto()).isEqualTo(2027);
    }

    // ── Distribución de montos ────────────────────────────────────────────────

    @Test
    void montoDivisibleExacto_todasLasCuotasIguales() {
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 10), 3)
                .montoEnPesos(new BigDecimal("300.00"))
                .build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        cuotas.forEach(c -> assertThat(c.getMontoEnPesos()).isEqualByComparingTo("100.00"));
    }

    @Test
    void montoConRedondeo_ultimaCuotaAbsorbeDiferencia() {
        // $100 en 3 cuotas → 33.33 + 33.33 + 33.34
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 10), 3)
                .montoEnPesos(new BigDecimal("100.00"))
                .build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas.get(0).getMontoEnPesos()).isEqualByComparingTo("33.33");
        assertThat(cuotas.get(1).getMontoEnPesos()).isEqualByComparingTo("33.33");
        assertThat(cuotas.get(2).getMontoEnPesos()).isEqualByComparingTo("33.34");

        BigDecimal sumaTotal = cuotas.stream()
                .map(CuotaImputada::getMontoEnPesos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumaTotal).isEqualByComparingTo("100.00");
    }

    // ── Override de fecha de cierre ───────────────────────────────────────────

    @Test
    void conOverrideReal_usaFechaCierreReal() {
        // El override dice que el cierre de junio 2026 fue el día 18, no el 20
        CierreTarjetaMes override = CierreTarjetaMes.builder()
                .tarjeta(tarjeta)
                .anio(2026).mes(6)
                .fechaCierreReal(LocalDate.of(2026, 6, 18))
                .fechaVencimientoReal(LocalDate.of(2026, 7, 8))
                .build();

        when(cierreTarjetaMesRepository.findByTarjetaAndAnioAndMes(eq(tarjeta), eq(2026), eq(6)))
                .thenReturn(Optional.of(override));

        // Compra el 19 (después del cierre real del 18) → salta a julio
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 6, 19), 1).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(7);
    }

    @Test
    void diaEstimado31EnFebrero_seClampea() {
        // Tarjeta con cierre estimado día 31, compra en febrero (28 días en 2026)
        tarjeta = TarjetaCredito.builder()
                .id(2L)
                .nombre("Master")
                .diaCierreEstimado(31)
                .diaVencimientoEstimado(15)
                .build();

        // Compra el 10 de febrero → cierre efectivo es 28-feb → no salta
        CompraTarjeta compra = compraBuilder(LocalDate.of(2026, 2, 10), 1).build();

        List<CuotaImputada> cuotas = service.generarCuotas(compra);

        assertThat(cuotas.get(0).getMesImpacto()).isEqualTo(2);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CompraTarjeta.CompraTarjetaBuilder compraBuilder(LocalDate fechaCompra, int cuotas) {
        return CompraTarjeta.builder()
                .tarjeta(tarjeta)
                .concepto("Test")
                .fechaCompra(fechaCompra)
                .monedaOriginal(Moneda.ARS)
                .cotizacionAplicada(BigDecimal.ONE)
                .montoOriginal(new BigDecimal("300.00"))
                .montoEnPesos(new BigDecimal("300.00"))
                .cantidadCuotas(cuotas);
    }
}

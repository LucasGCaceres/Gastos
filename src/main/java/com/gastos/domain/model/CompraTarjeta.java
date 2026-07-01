package com.gastos.domain.model;

import com.gastos.domain.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras_tarjeta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tarjeta_id")
    private TarjetaCredito tarjeta;

    @Column(nullable = false)
    private String concepto;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda_original", nullable = false)
    private Moneda monedaOriginal;

    // 1.0 si la compra es en ARS
    @Column(name = "cotizacion_aplicada", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal cotizacionAplicada = BigDecimal.ONE;

    @Column(name = "monto_original", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoOriginal;

    // montoOriginal × cotizacionAplicada
    @Column(name = "monto_en_pesos", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoEnPesos;

    @Column(name = "cantidad_cuotas", nullable = false)
    private Integer cantidadCuotas;

    @Column(name = "cuotas_ya_abonadas", nullable = false)
    @Builder.Default
    private Integer cuotasYaAbonadas = 0;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CuotaImputada> cuotas = new ArrayList<>();
}

package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ahorro_operaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AhorroOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ahorro_id", nullable = false)
    private CalculadoraAhorro ahorro;

    @Column(name = "objetivo_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal objetivoUsd;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal cotizacion;

    @Column(name = "monto_ars", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoArs;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate fecha = LocalDate.now();

    @Column(name = "mes_impacto", nullable = false)
    private Integer mesImpacto;

    @Column(name = "anio_impacto", nullable = false)
    private Integer anioImpacto;
}

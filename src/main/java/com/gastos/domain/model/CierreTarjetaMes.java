package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// Fechas reales de cierre y vencimiento de una tarjeta para un mes específico.
// Si no existe registro, el sistema usa los días estimados de TarjetaCredito como fallback.
@Entity
@Table(name = "cierres_tarjeta_mes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tarjeta_id", "anio", "mes"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CierreTarjetaMes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tarjeta_id")
    private TarjetaCredito tarjeta;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "fecha_cierre_real", nullable = false)
    private LocalDate fechaCierreReal;

    @Column(name = "fecha_vencimiento_real", nullable = false)
    private LocalDate fechaVencimientoReal;
}

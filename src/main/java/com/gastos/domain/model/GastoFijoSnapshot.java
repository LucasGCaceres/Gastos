package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Foto inmutable del monto de un gasto fijo para un ciclo cerrado.
@Entity
@Table(name = "gastos_fijos_snapshot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ciclo_mensual_id", "gasto_fijo_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GastoFijoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ciclo_mensual_id")
    private CicloMensual cicloMensual;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gasto_fijo_id")
    private GastoFijo gastoFijo;

    @Column(name = "monto_aplicado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoAplicado;
}

package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "gastos_fijos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GastoFijo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "monto_actual", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoActual;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}

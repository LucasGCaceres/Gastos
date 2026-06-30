package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tarjetas_credito")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TarjetaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String banco;

    @Column(name = "dia_cierre_estimado", nullable = false)
    private Integer diaCierreEstimado;

    @Column(name = "dia_vencimiento_estimado", nullable = false)
    private Integer diaVencimientoEstimado;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}

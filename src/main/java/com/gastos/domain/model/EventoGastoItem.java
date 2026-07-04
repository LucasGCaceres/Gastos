package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eventos_gasto_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoGastoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id")
    private EventoGasto evento;

    @Column(nullable = false)
    private String concepto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}

package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculadora_ahorro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalculadoraAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculadora_id", nullable = false, unique = true)
    private Calculadora calculadora;

    @Column(name = "objetivo_usd", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal objetivoUsd = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ahorro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha DESC, id DESC")
    @Builder.Default
    private List<AhorroOperacion> operaciones = new ArrayList<>();
}

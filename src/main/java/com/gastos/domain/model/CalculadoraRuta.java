package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculadora_rutas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalculadoraRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "calculadora_id")
    private Calculadora calculadora;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "viajes_por_mes", nullable = false)
    private Integer viajesPorMes;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<CalculadoraTramo> tramos = new ArrayList<>();
}

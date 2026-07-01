package com.gastos.domain.model;

import com.gastos.domain.enums.TipoCalculadora;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculadoras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Calculadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCalculadora tipo;

    @ManyToOne
    @JoinColumn(name = "gasto_fijo_id")
    private GastoFijo gastoFijo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @OneToMany(mappedBy = "calculadora", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<CalculadoraItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "calculadora", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalculadoraRuta> rutas = new ArrayList<>();

    @OneToOne(mappedBy = "calculadora", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CalculadoraAhorro ahorro;
}

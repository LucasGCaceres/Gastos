package com.gastos.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos_gasto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ciclo_mensual_id")
    private CicloMensual cicloMensual;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate fecha = LocalDate.now();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @Builder.Default
    private List<EventoGastoItem> items = new ArrayList<>();
}

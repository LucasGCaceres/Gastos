package com.gastos.domain.model;

import com.gastos.domain.enums.TipoInstrumento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "instrumentos_inversion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InstrumentoInversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInstrumento tipo;

    // Símbolo para consultar cotización en API externa (ej: "GD30", "BMA", "USD")
    private String ticker;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}

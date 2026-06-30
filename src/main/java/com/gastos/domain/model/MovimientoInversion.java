package com.gastos.domain.model;

import com.gastos.domain.enums.TipoMovimientoInversion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos_inversion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instrumento_id")
    private InstrumentoInversion instrumento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ciclo_mensual_id")
    private CicloMensual cicloMensual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimientoInversion tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    // Unidades compradas o vendidas (acciones, dólares, etc.)
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 15, scale = 4)
    private BigDecimal precioUnitario;

    @Column(name = "monto_total_pesos", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotalPesos;
}

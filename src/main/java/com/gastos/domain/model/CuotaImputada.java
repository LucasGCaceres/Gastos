package com.gastos.domain.model;

import com.gastos.domain.enums.EstadoCuota;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cuotas_imputadas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuotaImputada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "compra_id")
    private CompraTarjeta compra;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "monto_en_pesos", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoEnPesos;

    @Column(name = "mes_impacto", nullable = false)
    private Integer mesImpacto;

    @Column(name = "anio_impacto", nullable = false)
    private Integer anioImpacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoCuota estado = EstadoCuota.PENDIENTE;
}

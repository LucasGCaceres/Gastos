package com.gastos.domain.repository;

import com.gastos.domain.enums.EstadoCuota;
import com.gastos.domain.model.CuotaImputada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuotaImputadaRepository extends JpaRepository<CuotaImputada, Long> {

    // Todas las cuotas que impactan en un mes/año dado — usado para calcular totalCuotas del ciclo
    List<CuotaImputada> findByAnioImpactoAndMesImpacto(Integer anio, Integer mes);

    List<CuotaImputada> findByAnioImpactoAndMesImpactoAndEstado(Integer anio, Integer mes, EstadoCuota estado);
}

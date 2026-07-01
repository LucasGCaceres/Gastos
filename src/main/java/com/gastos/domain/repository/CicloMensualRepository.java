package com.gastos.domain.repository;

import com.gastos.domain.enums.EstadoCiclo;
import com.gastos.domain.model.CicloMensual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CicloMensualRepository extends JpaRepository<CicloMensual, Long> {

    Optional<CicloMensual> findByAnioAndMes(Integer anio, Integer mes);

    List<CicloMensual> findByEstadoOrderByAnioDescMesDesc(EstadoCiclo estado);

    boolean existsByAnioAndMes(Integer anio, Integer mes);

    List<CicloMensual> findAllByOrderByAnioDescMesDesc();
}

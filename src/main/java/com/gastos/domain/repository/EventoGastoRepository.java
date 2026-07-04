package com.gastos.domain.repository;

import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.EventoGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoGastoRepository extends JpaRepository<EventoGasto, Long> {

    List<EventoGasto> findByCicloMensualOrderByIdDesc(CicloMensual ciclo);

    List<EventoGasto> findByCicloMensual(CicloMensual ciclo);
}

package com.gastos.domain.repository;

import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.GastoFijoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoFijoSnapshotRepository extends JpaRepository<GastoFijoSnapshot, Long> {

    List<GastoFijoSnapshot> findByCicloMensual(CicloMensual cicloMensual);
}

package com.gastos.domain.repository;

import com.gastos.domain.model.Categoria;
import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.GastoVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoVariableRepository extends JpaRepository<GastoVariable, Long> {

    List<GastoVariable> findByCicloMensual(CicloMensual cicloMensual);

    List<GastoVariable> findByCicloMensualOrderByFechaDesc(CicloMensual cicloMensual);

    boolean existsByCategoria(Categoria categoria);
}

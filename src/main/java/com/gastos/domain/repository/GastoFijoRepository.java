package com.gastos.domain.repository;

import com.gastos.domain.model.GastoFijo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoFijoRepository extends JpaRepository<GastoFijo, Long> {

    List<GastoFijo> findByActivoTrue();
}

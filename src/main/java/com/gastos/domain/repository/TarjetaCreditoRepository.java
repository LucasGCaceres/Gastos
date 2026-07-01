package com.gastos.domain.repository;

import com.gastos.domain.model.TarjetaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarjetaCreditoRepository extends JpaRepository<TarjetaCredito, Long> {

    List<TarjetaCredito> findByActivaTrue();
}

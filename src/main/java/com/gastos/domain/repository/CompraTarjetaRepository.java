package com.gastos.domain.repository;

import com.gastos.domain.model.CompraTarjeta;
import com.gastos.domain.model.TarjetaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraTarjetaRepository extends JpaRepository<CompraTarjeta, Long> {

    List<CompraTarjeta> findByTarjeta(TarjetaCredito tarjeta);
}

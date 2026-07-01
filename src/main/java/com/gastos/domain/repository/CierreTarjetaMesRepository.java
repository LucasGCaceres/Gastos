package com.gastos.domain.repository;

import com.gastos.domain.model.CierreTarjetaMes;
import com.gastos.domain.model.TarjetaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CierreTarjetaMesRepository extends JpaRepository<CierreTarjetaMes, Long> {

    // Busca el override real; si no existe, el servicio cae al estimado de TarjetaCredito
    Optional<CierreTarjetaMes> findByTarjetaAndAnioAndMes(TarjetaCredito tarjeta, Integer anio, Integer mes);
}

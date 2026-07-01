package com.gastos.domain.repository;

import com.gastos.domain.model.CicloMensual;
import com.gastos.domain.model.InstrumentoInversion;
import com.gastos.domain.model.MovimientoInversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInversionRepository extends JpaRepository<MovimientoInversion, Long> {

    List<MovimientoInversion> findByInstrumento(InstrumentoInversion instrumento);

    List<MovimientoInversion> findByCicloMensual(CicloMensual cicloMensual);
}

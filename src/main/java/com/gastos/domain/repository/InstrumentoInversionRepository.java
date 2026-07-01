package com.gastos.domain.repository;

import com.gastos.domain.model.InstrumentoInversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentoInversionRepository extends JpaRepository<InstrumentoInversion, Long> {

    List<InstrumentoInversion> findByActivoTrue();
}

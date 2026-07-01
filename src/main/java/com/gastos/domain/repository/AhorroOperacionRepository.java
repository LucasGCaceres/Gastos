package com.gastos.domain.repository;

import com.gastos.domain.model.AhorroOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AhorroOperacionRepository extends JpaRepository<AhorroOperacion, Long> {
}

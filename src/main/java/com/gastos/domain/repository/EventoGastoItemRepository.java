package com.gastos.domain.repository;

import com.gastos.domain.model.EventoGastoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoGastoItemRepository extends JpaRepository<EventoGastoItem, Long> {
}

package com.gastos.domain.repository;

import com.gastos.domain.model.Calculadora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculadoraRepository extends JpaRepository<Calculadora, Long> {
    List<Calculadora> findByActivaTrueOrderByNombreAsc();
}

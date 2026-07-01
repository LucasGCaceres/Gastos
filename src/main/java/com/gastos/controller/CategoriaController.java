package com.gastos.controller;

import com.gastos.domain.repository.CategoriaRepository;
import com.gastos.dto.response.CategoriaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepo;

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaRepo.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getIcono()))
                .toList();
    }
}

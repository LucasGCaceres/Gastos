package com.gastos.controller;

import com.gastos.dto.request.CrearCategoriaRequest;
import com.gastos.dto.response.CategoriaResponse;
import com.gastos.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CrearCategoriaRequest req) {
        return categoriaService.crear(req);
    }

    @PatchMapping("/{id}")
    public CategoriaResponse editar(@PathVariable Long id, @Valid @RequestBody CrearCategoriaRequest req) {
        return categoriaService.editar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}

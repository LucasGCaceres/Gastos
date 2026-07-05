package com.gastos.service;

import com.gastos.domain.model.Categoria;
import com.gastos.domain.repository.CategoriaRepository;
import com.gastos.domain.repository.GastoVariableRepository;
import com.gastos.dto.request.CrearCategoriaRequest;
import com.gastos.dto.response.CategoriaResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepo;
    private final GastoVariableRepository gastoVariableRepo;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoriaResponse crear(CrearCategoriaRequest req) {
        categoriaRepo.findByNombre(req.nombre()).ifPresent(c -> {
            throw new IllegalStateException("Ya existe una categoría llamada \"" + req.nombre() + "\"");
        });
        Categoria categoria = Categoria.builder()
                .nombre(req.nombre())
                .icono(req.icono())
                .build();
        return toResponse(categoriaRepo.save(categoria));
    }

    @Transactional
    public CategoriaResponse editar(Long id, CrearCategoriaRequest req) {
        Categoria categoria = get(id);
        categoriaRepo.findByNombre(req.nombre()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalStateException("Ya existe una categoría llamada \"" + req.nombre() + "\"");
            }
        });
        categoria.setNombre(req.nombre());
        categoria.setIcono(req.icono());
        return toResponse(categoriaRepo.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = get(id);
        if (gastoVariableRepo.existsByCategoria(categoria)) {
            throw new IllegalStateException(
                    "No se puede eliminar \"" + categoria.getNombre() + "\": hay gastos variables que la usan");
        }
        categoriaRepo.delete(categoria);
    }

    private Categoria get(Long id) {
        return categoriaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
    }

    private CategoriaResponse toResponse(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getIcono());
    }
}

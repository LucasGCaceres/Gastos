package com.gastos.controller;

import com.gastos.dto.request.ActualizarEstadoTarjetaRequest;
import com.gastos.dto.request.CrearTarjetaRequest;
import com.gastos.dto.request.SetCierreTarjetaMesRequest;
import com.gastos.dto.response.CierreTarjetaMesResponse;
import com.gastos.dto.response.TarjetaDeudaResponse;
import com.gastos.dto.response.TarjetaResponse;
import com.gastos.service.TarjetaCreditoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarjetas")
@RequiredArgsConstructor
public class TarjetaCreditoController {

    private final TarjetaCreditoService tarjetaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarjetaResponse crear(@Valid @RequestBody CrearTarjetaRequest req) {
        return tarjetaService.crear(req);
    }

    @GetMapping
    public List<TarjetaResponse> listar() {
        return tarjetaService.listarActivas();
    }

    @GetMapping("/todas")
    public List<TarjetaResponse> listarTodas() {
        return tarjetaService.listarTodas();
    }

    @PutMapping("/{id}")
    public TarjetaResponse editar(@PathVariable Long id, @Valid @RequestBody CrearTarjetaRequest req) {
        return tarjetaService.editar(id, req);
    }

    @PutMapping("/{id}/cierres")
    public CierreTarjetaMesResponse setCierreMes(@PathVariable Long id,
                                                  @Valid @RequestBody SetCierreTarjetaMesRequest req) {
        return tarjetaService.setCierreMes(id, req);
    }

    @GetMapping("/{id}/cierres")
    public CierreTarjetaMesResponse getCierreMes(@PathVariable Long id,
                                                  @RequestParam Integer anio,
                                                  @RequestParam Integer mes) {
        return tarjetaService.getCierreMes(id, anio, mes);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        tarjetaService.desactivar(id);
    }

    @PatchMapping("/{id}/estado")
    public TarjetaResponse actualizarEstado(@PathVariable Long id,
                                             @Valid @RequestBody ActualizarEstadoTarjetaRequest req) {
        return tarjetaService.actualizarEstado(id, req.activa());
    }

    @GetMapping("/deudas")
    public List<TarjetaDeudaResponse> desgloseDeudas(@RequestParam Integer anio, @RequestParam Integer mes) {
        return tarjetaService.desgloseDeudas(anio, mes);
    }
}

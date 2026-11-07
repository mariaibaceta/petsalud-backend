package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.VentaDtos.VentaResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.VentaService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** Un CLIENTE solo puede comprar a su nombre (usuarioId = su propio id). */
    @PostMapping
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#usuarioId)")
    public ResponseEntity<VentaResponse> realizarVenta(
            @RequestParam Long usuarioId,
            @RequestBody @NotEmpty @Size(max = 50) List<@NotNull @Positive Long> productosIds) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VentaResponse.de(ventaService.realizarVenta(usuarioId, productosIds)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioVenta(#id)")
    public ResponseEntity<VentaResponse> obtenerVentaPorId(@PathVariable Long id) {
        return ventaService.obtenerVentaPorId(id)
                .map(v -> ResponseEntity.ok(VentaResponse.de(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#usuarioId)")
    public ResponseEntity<List<VentaResponse>> listarVentasPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ventaService.listarVentasPorUsuario(usuarioId).stream().map(VentaResponse::de).toList());
    }

    /** Reporte comercial de todas las ventas: exclusivo de gerencia. */
    @GetMapping
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<List<VentaResponse>> listarTodasLasVentas() {
        return ResponseEntity.ok(ventaService.listarTodasLasVentas().stream().map(VentaResponse::de).toList());
    }
}

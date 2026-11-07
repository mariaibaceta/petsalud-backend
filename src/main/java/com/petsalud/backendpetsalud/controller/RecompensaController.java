package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.RecompensaDtos.RecompensaRequest;
import com.petsalud.backendpetsalud.dto.RecompensaDtos.RecompensaResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.RecompensaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recompensas")
public class RecompensaController {

    private final RecompensaService recompensaService;

    public RecompensaController(RecompensaService recompensaService) {
        this.recompensaService = recompensaService;
    }

    @PostMapping("/usuario/{usuarioId}")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<RecompensaResponse> agregarRecompensa(@PathVariable Long usuarioId,
                                                                @Valid @RequestBody RecompensaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RecompensaResponse.de(recompensaService.agregarRecompensa(usuarioId, solicitud.aEntidad())));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioRecompensa(#id)")
    public ResponseEntity<RecompensaResponse> obtenerRecompensaPorId(@PathVariable Long id) {
        return recompensaService.obtenerRecompensaPorId(id)
                .map(r -> ResponseEntity.ok(RecompensaResponse.de(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#usuarioId)")
    public ResponseEntity<List<RecompensaResponse>> listarRecompensasPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(recompensaService.listarRecompensasPorUsuario(usuarioId).stream()
                .map(RecompensaResponse::de).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<Void> eliminarRecompensa(@PathVariable Long id) {
        recompensaService.eliminarRecompensa(id);
        return ResponseEntity.noContent().build();
    }
}

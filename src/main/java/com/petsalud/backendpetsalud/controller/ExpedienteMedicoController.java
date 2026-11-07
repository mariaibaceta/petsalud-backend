package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.ExpedienteDtos.ExpedienteRequest;
import com.petsalud.backendpetsalud.dto.ExpedienteDtos.ExpedienteResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Datos clínicos: solo personal escribe; el CLIENTE únicamente lee los expedientes de sus mascotas. */
@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteMedicoController {

    private final ExpedienteMedicoService expedienteMedicoService;

    public ExpedienteMedicoController(ExpedienteMedicoService expedienteMedicoService) {
        this.expedienteMedicoService = expedienteMedicoService;
    }

    @PostMapping
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<ExpedienteResponse> agregarRegistro(@RequestParam Long mascotaId,
                                                              @RequestParam Long veterinarioId,
                                                              @Valid @RequestBody ExpedienteRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpedienteResponse.de(
                expedienteMedicoService.agregarRegistro(mascotaId, veterinarioId, solicitud.aEntidad())));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioExpediente(#id)")
    public ResponseEntity<ExpedienteResponse> obtenerRegistroPorId(@PathVariable Long id) {
        return expedienteMedicoService.obtenerRegistroPorId(id)
                .map(e -> ResponseEntity.ok(ExpedienteResponse.de(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioMascota(#mascotaId)")
    public ResponseEntity<List<ExpedienteResponse>> listarRegistrosPorMascota(@PathVariable Long mascotaId) {
        return ResponseEntity.ok(expedienteMedicoService.listarRegistrosPorMascota(mascotaId).stream()
                .map(ExpedienteResponse::de).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<Void> eliminarRegistro(@PathVariable Long id) {
        expedienteMedicoService.eliminarRegistro(id);
        return ResponseEntity.noContent().build();
    }
}

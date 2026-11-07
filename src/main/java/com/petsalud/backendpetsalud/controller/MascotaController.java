package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.MascotaDtos.MascotaRequest;
import com.petsalud.backendpetsalud.dto.MascotaDtos.MascotaResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping("/propietario/{propietarioId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#propietarioId)")
    public ResponseEntity<MascotaResponse> registrarMascota(@PathVariable Long propietarioId,
                                                            @Valid @RequestBody MascotaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MascotaResponse.de(mascotaService.registrarMascota(solicitud.aEntidad(), propietarioId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioMascota(#id)")
    public ResponseEntity<MascotaResponse> obtenerMascotaPorId(@PathVariable Long id) {
        return mascotaService.obtenerMascotaPorId(id)
                .map(m -> ResponseEntity.ok(MascotaResponse.de(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioMascota(#id)")
    public ResponseEntity<MascotaResponse> actualizarMascota(@PathVariable Long id,
                                                             @Valid @RequestBody MascotaRequest solicitud) {
        return ResponseEntity.ok(MascotaResponse.de(mascotaService.actualizarMascota(id, solicitud.aEntidad())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE + " or @authz.esPropietarioMascota(#id)")
    public ResponseEntity<Void> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminarMascota(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/propietario/{propietarioId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#propietarioId)")
    public ResponseEntity<List<MascotaResponse>> listarMascotasPorPropietario(@PathVariable Long propietarioId) {
        return ResponseEntity.ok(mascotaService.listarMascotasPorPropietario(propietarioId).stream()
                .map(MascotaResponse::de).toList());
    }
}

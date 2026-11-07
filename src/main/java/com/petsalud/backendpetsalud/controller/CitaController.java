package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.CitaDtos.CitaResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.CitaService;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /** Un CLIENTE solo puede agendar para sus propias mascotas; el veterinario debe ser EMPLEADO. */
    @PostMapping
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioMascota(#mascotaId)")
    public ResponseEntity<CitaResponse> agendarCita(
            @RequestParam Long mascotaId,
            @RequestParam Long veterinarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Future LocalDateTime fechaHora,
            @RequestParam @NotBlank @Size(max = 300) @TextoSeguro String motivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CitaResponse.de(citaService.agendarCita(mascotaId, veterinarioId, fechaHora, motivo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioCita(#id)")
    public ResponseEntity<CitaResponse> obtenerCitaPorId(@PathVariable Long id) {
        return citaService.obtenerCitaPorId(id)
                .map(c -> ResponseEntity.ok(CitaResponse.de(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<CitaResponse> actualizarEstadoCita(
            @PathVariable Long id,
            @RequestParam @Pattern(regexp = "^(Agendada|Confirmada|Completada|Cancelada)$",
                    message = "debe ser Agendada, Confirmada, Completada o Cancelada") String estado) {
        return ResponseEntity.ok(CitaResponse.de(citaService.actualizarEstadoCita(id, estado)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioCita(#id)")
    public ResponseEntity<Void> cancelarCita(@PathVariable Long id) {
        citaService.cancelarCita(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioMascota(#mascotaId)")
    public ResponseEntity<List<CitaResponse>> listarCitasPorMascota(@PathVariable Long mascotaId) {
        return ResponseEntity.ok(citaService.listarCitasPorMascota(mascotaId).stream().map(CitaResponse::de).toList());
    }

    @GetMapping("/veterinario/{veterinarioId}")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<List<CitaResponse>> listarCitasPorVeterinario(@PathVariable Long veterinarioId) {
        return ResponseEntity.ok(citaService.listarCitasPorVeterinario(veterinarioId).stream().map(CitaResponse::de).toList());
    }

    @GetMapping("/entre-fechas")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<List<CitaResponse>> listarCitasEntreFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(citaService.listarCitasEntreFechas(inicio, fin).stream().map(CitaResponse::de).toList());
    }
}

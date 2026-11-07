package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ExpedienteDtos {

    private ExpedienteDtos() {
    }

    public record ExpedienteRequest(
            @NotBlank @Size(max = 500) @TextoSeguro String descripcion,
            @NotBlank @Size(max = 500) @TextoSeguro String diagnostico,
            @Size(max = 500) @TextoSeguro String tratamiento) {

        public ExpedienteMedico aEntidad() {
            return ExpedienteMedico.builder().descripcion(descripcion).diagnostico(diagnostico)
                    .tratamiento(tratamiento).build();
        }
    }

    public record ExpedienteResponse(Long id, LocalDateTime fecha, String descripcion, String diagnostico,
                                     String tratamiento, Long mascotaId, Long veterinarioId) {
        public static ExpedienteResponse de(ExpedienteMedico e) {
            return new ExpedienteResponse(e.getId(), e.getFecha(), e.getDescripcion(), e.getDiagnostico(),
                    e.getTratamiento(), e.getMascota() != null ? e.getMascota().getId() : null,
                    e.getVeterinario() != null ? e.getVeterinario().getId() : null);
        }
    }
}

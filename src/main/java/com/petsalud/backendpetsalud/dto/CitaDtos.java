package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Cita;

import java.time.LocalDateTime;

public final class CitaDtos {

    private CitaDtos() {
    }

    public record CitaResponse(Long id, LocalDateTime fechaHora, String estado, String motivo, Long mascotaId,
                               String mascotaNombre, Long veterinarioId, String veterinarioNombre) {
        public static CitaResponse de(Cita c) {
            return new CitaResponse(c.getId(), c.getFechaHora(), c.getEstado(), c.getMotivo(),
                    c.getMascota() != null ? c.getMascota().getId() : null,
                    c.getMascota() != null ? c.getMascota().getNombre() : null,
                    c.getVeterinario() != null ? c.getVeterinario().getId() : null,
                    c.getVeterinario() != null ? c.getVeterinario().getNombre() + " "
                            + (c.getVeterinario().getApellido() == null ? "" : c.getVeterinario().getApellido()).trim() : null);
        }
    }
}

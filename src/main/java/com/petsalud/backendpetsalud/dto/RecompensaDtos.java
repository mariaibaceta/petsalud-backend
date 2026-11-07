package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Recompensa;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class RecompensaDtos {

    private RecompensaDtos() {
    }

    public record RecompensaRequest(
            @NotNull @Positive @Max(100000) Integer puntos,
            @NotBlank @Size(max = 200) @TextoSeguro String descripcion) {

        public Recompensa aEntidad() {
            return Recompensa.builder().puntos(puntos).descripcion(descripcion).build();
        }
    }

    public record RecompensaResponse(Long id, Integer puntos, String descripcion, LocalDateTime fecha, Long usuarioId) {
        public static RecompensaResponse de(Recompensa r) {
            return new RecompensaResponse(r.getId(), r.getPuntos(), r.getDescripcion(), r.getFecha(),
                    r.getUsuario() != null ? r.getUsuario().getId() : null);
        }
    }
}

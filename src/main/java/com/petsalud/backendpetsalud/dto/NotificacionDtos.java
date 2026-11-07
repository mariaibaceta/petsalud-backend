package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Notificacion;
import com.petsalud.backendpetsalud.validation.TextoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class NotificacionDtos {

    private NotificacionDtos() {
    }

    public record NotificacionRequest(
            @NotBlank @Size(max = 300) @TextoSeguro String mensaje,
            @NotBlank @Pattern(regexp = "^(Promoción|Recordatorio|Evento|Adopción)$",
                    message = "debe ser Promoción, Recordatorio, Evento o Adopción") String tipo) {

        public Notificacion aEntidad() {
            return Notificacion.builder().mensaje(mensaje).tipo(tipo).build();
        }
    }

    public record NotificacionResponse(Long id, String mensaje, String tipo, LocalDateTime fecha, Boolean leida,
                                       Long usuarioId) {
        public static NotificacionResponse de(Notificacion n) {
            return new NotificacionResponse(n.getId(), n.getMensaje(), n.getTipo(), n.getFecha(), n.getLeida(),
                    n.getUsuario() != null ? n.getUsuario().getId() : null);
        }
    }
}

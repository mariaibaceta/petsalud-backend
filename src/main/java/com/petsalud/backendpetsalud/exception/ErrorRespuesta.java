package com.petsalud.backendpetsalud.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error de la API. Nunca incluye trazas, nombres de clases ni mensajes internos.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorRespuesta(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        List<String> detalles) {

    public static ErrorRespuesta de(int status, String error, String mensaje, String ruta) {
        return new ErrorRespuesta(Instant.now(), status, error, mensaje, ruta, List.of());
    }

    public static ErrorRespuesta de(int status, String error, String mensaje, String ruta, List<String> detalles) {
        return new ErrorRespuesta(Instant.now(), status, error, mensaje, ruta, detalles);
    }
}

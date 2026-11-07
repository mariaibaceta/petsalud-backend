package com.petsalud.backendpetsalud.exception;

/** La solicitud es válida en forma pero viola una regla del negocio (respuesta 400). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}

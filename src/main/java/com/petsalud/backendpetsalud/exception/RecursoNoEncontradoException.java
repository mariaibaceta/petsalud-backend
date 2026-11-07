package com.petsalud.backendpetsalud.exception;

/** El recurso solicitado no existe (respuesta 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

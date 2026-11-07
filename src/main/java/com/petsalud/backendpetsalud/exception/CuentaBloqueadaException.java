package com.petsalud.backendpetsalud.exception;

/** Demasiados intentos fallidos de login: bloqueo temporal (respuesta 429). */
public class CuentaBloqueadaException extends RuntimeException {
    private final long segundosRestantes;

    public CuentaBloqueadaException(long segundosRestantes) {
        super("Demasiados intentos fallidos. Intente nuevamente más tarde.");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}

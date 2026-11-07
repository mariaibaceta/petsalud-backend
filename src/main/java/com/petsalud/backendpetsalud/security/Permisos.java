package com.petsalud.backendpetsalud.security;

/**
 * Expresiones SpEL reutilizadas en {@code @PreAuthorize}. Mantenerlas en un solo lugar evita errores
 * de tipeo y deja la matriz de autorización legible en cada controlador.
 */
public final class Permisos {

    /** Personal de la clínica: EMPLEADO o GERENTE. */
    public static final String PERSONAL = "hasAnyRole('EMPLEADO','GERENTE')";
    /** Solo gerencia. */
    public static final String GERENTE = "hasRole('GERENTE')";
    /** Cualquier usuario con JWT válido. */
    public static final String AUTENTICADO = "isAuthenticated()";

    private Permisos() {
    }
}

package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 en JSON cuando se intenta acceder a un recurso protegido sin un JWT válido. */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Atributo que el filtro JWT usa para indicar que el token venía pero era inválido. */
    public static final String ATRIBUTO_TOKEN_INVALIDO = "petsalud.tokenInvalido";

    private final EscritorErrorJson escritor;
    private final AuditoriaSeguridad auditoria;

    public JwtAuthenticationEntryPoint(EscritorErrorJson escritor, AuditoriaSeguridad auditoria) {
        this.escritor = escritor;
        this.auditoria = auditoria;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean tokenInvalido = request.getAttribute(ATRIBUTO_TOKEN_INVALIDO) != null;
        if (!tokenInvalido) {
            auditoria.registrar(AuditoriaSeguridad.Evento.ACCESO_NO_AUTENTICADO, null, request, "sin token Bearer");
        }
        String mensaje = tokenInvalido
                ? "Token inválido o expirado"
                : "Se requiere autenticación (cabecera Authorization: Bearer <token>)";
        response.setHeader("WWW-Authenticate", "Bearer");
        escritor.escribir(response, ErrorRespuesta.de(401, "Unauthorized", mensaje, request.getRequestURI()));
    }
}

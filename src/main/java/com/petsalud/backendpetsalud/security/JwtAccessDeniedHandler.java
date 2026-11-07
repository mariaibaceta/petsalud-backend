package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403 en JSON cuando un usuario autenticado no tiene el rol o la propiedad requerida. */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final EscritorErrorJson escritor;
    private final AuditoriaSeguridad auditoria;

    public JwtAccessDeniedHandler(EscritorErrorJson escritor, AuditoriaSeguridad auditoria) {
        this.escritor = escritor;
        this.auditoria = auditoria;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null ? auth.getName() : null;
        String rol = auth != null ? auth.getAuthorities().toString() : "-";
        auditoria.registrar(AuditoriaSeguridad.Evento.ACCESO_DENEGADO, usuario, request, "rol=" + rol);
        escritor.escribir(response, ErrorRespuesta.de(403, "Forbidden",
                "No tiene permisos para acceder a este recurso", request.getRequestURI()));
    }
}

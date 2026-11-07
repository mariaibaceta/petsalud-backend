package com.petsalud.backendpetsalud.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Monitoreo básico de actividad sospechosa.
 * Escribe eventos de seguridad en el logger "SEGURIDAD", que logback envía al archivo dedicado
 * logs/seguridad.log (ver logback-spring.xml).
 * <p>
 * Reglas de privacidad: nunca se registran contraseñas ni tokens; el email se enmascara
 * y se neutralizan saltos de línea para evitar inyección en logs (CWE-117).
 */
@Component
public class AuditoriaSeguridad {

    public enum Evento {
        LOGIN_EXITOSO, LOGIN_FALLIDO, CUENTA_BLOQUEADA, REGISTRO_USUARIO,
        TOKEN_INVALIDO, ACCESO_NO_AUTENTICADO, ACCESO_DENEGADO,
        CAMBIO_ROL, USUARIO_ELIMINADO
    }

    private static final Logger LOG = LoggerFactory.getLogger("SEGURIDAD");

    public void registrar(Evento evento, String usuario, HttpServletRequest request, String detalle) {
        String linea = String.format("evento=%s usuario=%s ip=%s metodo=%s ruta=%s detalle=\"%s\"",
                evento,
                enmascararEmail(usuario),
                limpiar(ipCliente(request)),
                request != null ? request.getMethod() : "-",
                limpiar(request != null ? request.getRequestURI() : "-"),
                limpiar(detalle));
        switch (evento) {
            case LOGIN_EXITOSO, REGISTRO_USUARIO -> LOG.info(linea);
            default -> LOG.warn(linea);
        }
    }

    /** IP de origen. Nota: si se despliega detrás de un proxy, configurar server.forward-headers-strategy. */
    public static String ipCliente(HttpServletRequest request) {
        return request == null ? "-" : request.getRemoteAddr();
    }

    /** ana.perez@cliente.cl -> an***@cliente.cl (minimización de datos personales en logs). */
    static String enmascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "anonimo";
        }
        String e = limpiar(email.trim().toLowerCase(Locale.ROOT));
        int arroba = e.indexOf('@');
        if (arroba <= 0) {
            return e.length() <= 2 ? "***" : e.substring(0, 2) + "***";
        }
        String local = e.substring(0, arroba);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + e.substring(arroba);
    }

    /** Neutraliza CR/LF y caracteres de control, y limita el largo. */
    static String limpiar(String valor) {
        if (valor == null) {
            return "-";
        }
        String limpio = valor.replaceAll("[\\r\\n\\t\\p{Cntrl}]", "_").replace("\"", "'");
        return limpio.length() > 200 ? limpio.substring(0, 200) + "..." : limpio;
    }
}

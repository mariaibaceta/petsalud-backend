package com.petsalud.backendpetsalud.exception;

import com.petsalud.backendpetsalud.security.AuditoriaSeguridad;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Manejo global de errores: respuestas JSON homogéneas y genéricas.
 * Nunca se devuelven trazas, nombres de clases, SQL ni mensajes internos (mitiga fuga de información).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditoriaSeguridad auditoria;

    public GlobalExceptionHandler(AuditoriaSeguridad auditoria) {
        this.auditoria = auditoria;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacionBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted().toList();
        return error(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", req, detalles);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorRespuesta> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .sorted().toList();
        return error(HttpStatus.BAD_REQUEST, "Parámetros inválidos", req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> validacion(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getConstraintViolations().stream().map(v -> v.getMessage()).sorted().toList();
        return error(HttpStatus.BAD_REQUEST, "Parámetros inválidos", req, detalles);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorRespuesta> parametroIncorrecto(Exception ex, HttpServletRequest req) {
        String nombre = ex instanceof MethodArgumentTypeMismatchException m ? m.getName()
                : ((MissingServletRequestParameterException) ex).getParameterName();
        return error(HttpStatus.BAD_REQUEST, "Parámetro ausente o con formato incorrecto: " + nombre, req, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", req, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> tipoNoSoportado(HttpServletRequest req) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type no soportado (use application/json)", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoSoportado(HttpServletRequest req) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", req, List.of());
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorRespuesta> noEncontrado(Exception ex, HttpServletRequest req) {
        String mensaje = ex instanceof RecursoNoEncontradoException ? ex.getMessage() : "Recurso no encontrado";
        return error(HttpStatus.NOT_FOUND, mensaje, req, List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        LOG.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getClass().getSimpleName());
        return error(HttpStatus.CONFLICT, "La operación entra en conflicto con datos relacionados", req, List.of());
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorRespuesta> bloqueada(CuentaBloqueadaException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosRestantes()))
                .body(ErrorRespuesta.de(429, "Too Many Requests", ex.getMessage(), req.getRequestURI()));
    }

    /** Credenciales incorrectas en el login: mensaje genérico (no revela si el email existe). */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorRespuesta> credenciales(HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", req, List.of());
    }

    /** @PreAuthorize denegado dentro de un controlador: 403 + registro en el log de seguridad. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> accesoDenegado(HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        auditoria.registrar(AuditoriaSeguridad.Evento.ACCESO_DENEGADO, auth != null ? auth.getName() : null, req,
                "rol=" + (auth != null ? auth.getAuthorities() : "-") + " (regla @PreAuthorize)");
        return error(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso", req, List.of());
    }

    /** Cualquier error no previsto: 500 genérico; el detalle queda solo en el log del servidor. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex, HttpServletRequest req) {
        LOG.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno. Contacte al administrador.", req, List.of());
    }

    private static ResponseEntity<ErrorRespuesta> error(HttpStatus status, String mensaje, HttpServletRequest req,
                                                        List<String> detalles) {
        return ResponseEntity.status(status)
                .body(ErrorRespuesta.de(status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI(), detalles));
    }
}

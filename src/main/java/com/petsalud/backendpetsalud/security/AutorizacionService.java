package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.NotificacionRepository;
import com.petsalud.backendpetsalud.repository.RecompensaRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verificación de PROPIEDAD de recursos (mitiga IDOR / acceso no autorizado a datos sensibles).
 * Se invoca desde las expresiones {@code @PreAuthorize} con el nombre de bean {@code authz}, por ejemplo:
 * <pre>@PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @authz.esPropietarioMascota(#id)")</pre>
 * Si el recurso no existe o pertenece a otro usuario, devuelve {@code false} (respuesta 403), de modo que
 * un cliente no puede distinguir "no existe" de "no es tuyo" (evita enumeración de IDs).
 */
@Component("authz")
@Transactional(readOnly = true)
public class AutorizacionService {

    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ExpedienteMedicoRepository expedienteRepository;
    private final VentaRepository ventaRepository;
    private final RecompensaRepository recompensaRepository;
    private final NotificacionRepository notificacionRepository;

    public AutorizacionService(MascotaRepository mascotaRepository, CitaRepository citaRepository,
                               ExpedienteMedicoRepository expedienteRepository, VentaRepository ventaRepository,
                               RecompensaRepository recompensaRepository, NotificacionRepository notificacionRepository) {
        this.mascotaRepository = mascotaRepository;
        this.citaRepository = citaRepository;
        this.expedienteRepository = expedienteRepository;
        this.ventaRepository = ventaRepository;
        this.recompensaRepository = recompensaRepository;
        this.notificacionRepository = notificacionRepository;
    }

    /** El id corresponde al usuario autenticado. */
    public boolean esUsuarioActual(Long usuarioId) {
        Long actual = idUsuarioActual();
        return actual != null && actual.equals(usuarioId);
    }

    public boolean esPropietarioMascota(Long mascotaId) {
        Long actual = idUsuarioActual();
        return actual != null && mascotaId != null && mascotaRepository.existsByIdAndPropietarioId(mascotaId, actual);
    }

    public boolean esPropietarioCita(Long citaId) {
        Long actual = idUsuarioActual();
        return actual != null && citaId != null && citaRepository.existsByIdAndMascotaPropietarioId(citaId, actual);
    }

    public boolean esPropietarioExpediente(Long expedienteId) {
        Long actual = idUsuarioActual();
        return actual != null && expedienteId != null
                && expedienteRepository.existsByIdAndMascotaPropietarioId(expedienteId, actual);
    }

    public boolean esPropietarioVenta(Long ventaId) {
        Long actual = idUsuarioActual();
        return actual != null && ventaId != null && ventaRepository.existsByIdAndUsuarioId(ventaId, actual);
    }

    public boolean esPropietarioRecompensa(Long recompensaId) {
        Long actual = idUsuarioActual();
        return actual != null && recompensaId != null && recompensaRepository.existsByIdAndUsuarioId(recompensaId, actual);
    }

    public boolean esPropietarioNotificacion(Long notificacionId) {
        Long actual = idUsuarioActual();
        return actual != null && notificacionId != null
                && notificacionRepository.existsByIdAndUsuarioId(notificacionId, actual);
    }

    private Long idUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioDetails usuario) {
            return usuario.getId();
        }
        return null;
    }
}

package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.NotificacionDtos.NotificacionRequest;
import com.petsalud.backendpetsalud.dto.NotificacionDtos.NotificacionResponse;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/usuario/{usuarioId}")
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<NotificacionResponse> enviarNotificacion(@PathVariable Long usuarioId,
                                                                   @Valid @RequestBody NotificacionRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificacionResponse.de(notificacionService.enviarNotificacion(usuarioId, solicitud.aEntidad())));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esPropietarioNotificacion(#id)")
    public ResponseEntity<NotificacionResponse> obtenerNotificacionPorId(@PathVariable Long id) {
        return notificacionService.obtenerNotificacionPorId(id)
                .map(n -> ResponseEntity.ok(NotificacionResponse.de(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#usuarioId)")
    public ResponseEntity<List<NotificacionResponse>> listarNotificacionesPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarNotificacionesPorUsuario(usuarioId).stream()
                .map(NotificacionResponse::de).toList());
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#usuarioId)")
    public ResponseEntity<List<NotificacionResponse>> listarNotificacionesNoLeidas(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarNotificacionesNoLeidas(usuarioId).stream()
                .map(NotificacionResponse::de).toList());
    }

    @PutMapping("/{id}/leida")
    @PreAuthorize(Permisos.GERENTE + " or @authz.esPropietarioNotificacion(#id)")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE + " or @authz.esPropietarioNotificacion(#id)")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id) {
        notificacionService.eliminarNotificacion(id);
        return ResponseEntity.noContent().build();
    }
}

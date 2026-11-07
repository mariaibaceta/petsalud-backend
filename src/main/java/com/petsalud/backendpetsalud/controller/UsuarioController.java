package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.UsuarioDtos.ActualizarUsuarioRequest;
import com.petsalud.backendpetsalud.dto.UsuarioDtos.CambioRolRequest;
import com.petsalud.backendpetsalud.dto.UsuarioDtos.RegistroRequest;
import com.petsalud.backendpetsalud.dto.UsuarioDtos.UsuarioResponse;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.AuditoriaSeguridad;
import com.petsalud.backendpetsalud.security.AuditoriaSeguridad.Evento;
import com.petsalud.backendpetsalud.security.Permisos;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import com.petsalud.backendpetsalud.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaSeguridad auditoria;

    public UsuarioController(UsuarioService usuarioService, AuditoriaSeguridad auditoria) {
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
    }

    /** Público. El rol se fuerza a CLIENTE y la contraseña se almacena con BCrypt. */
    @PostMapping("/registro")
    @PreAuthorize("permitAll()")
    public ResponseEntity<UsuarioResponse> registrarUsuario(@Valid @RequestBody RegistroRequest solicitud,
                                                            HttpServletRequest request) {
        Usuario nuevo = usuarioService.registrarUsuario(solicitud);
        auditoria.registrar(Evento.REGISTRO_USUARIO, nuevo.getEmail(), request, "rol=CLIENTE");
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(nuevo));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permisos.PERSONAL + " or @authz.esUsuarioActual(#id)")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(u -> ResponseEntity.ok(UsuarioResponse.de(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE + " or @authz.esUsuarioActual(#id)")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id,
                                                             @Valid @RequestBody ActualizarUsuarioRequest solicitud) {
        return ResponseEntity.ok(UsuarioResponse.de(usuarioService.actualizarUsuario(id, solicitud)));
    }

    /** Asignación de roles (CLIENTE, EMPLEADO, GERENTE): exclusiva del gerente. */
    @PutMapping("/{id}/rol")
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id, @Valid @RequestBody CambioRolRequest solicitud,
                                                      @AuthenticationPrincipal UsuarioDetails actual,
                                                      HttpServletRequest request) {
        Usuario usuario = usuarioService.cambiarRol(id, solicitud.rol(), actual.getId());
        auditoria.registrar(Evento.CAMBIO_ROL, actual.getUsername(), request,
                "usuarioId=" + id + " nuevoRol=" + solicitud.rol());
        return ResponseEntity.ok(UsuarioResponse.de(usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.GERENTE)
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id, @AuthenticationPrincipal UsuarioDetails actual,
                                                HttpServletRequest request) {
        usuarioService.eliminarUsuario(id, actual.getId());
        auditoria.registrar(Evento.USUARIO_ELIMINADO, actual.getUsername(), request, "usuarioId=" + id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize(Permisos.PERSONAL)
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios().stream().map(UsuarioResponse::de).toList());
    }
}

package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.AuthDtos.LoginRequest;
import com.petsalud.backendpetsalud.dto.AuthDtos.LoginResponse;
import com.petsalud.backendpetsalud.exception.CuentaBloqueadaException;
import com.petsalud.backendpetsalud.security.AuditoriaSeguridad;
import com.petsalud.backendpetsalud.security.AuditoriaSeguridad.Evento;
import com.petsalud.backendpetsalud.security.IntentosLoginService;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * Inicio de sesión: valida credenciales contra la BD (UsuarioDetailsService + BCrypt) y emite un JWT.
 * Incluye bloqueo temporal por intentos fallidos y registro de cada intento en el log de seguridad.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IntentosLoginService intentos;
    private final AuditoriaSeguridad auditoria;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          IntentosLoginService intentos, AuditoriaSeguridad auditoria) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.intentos = intentos;
        this.auditoria = auditoria;
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest solicitud, HttpServletRequest request) {
        String email = solicitud.email().trim().toLowerCase(Locale.ROOT);
        String ip = AuditoriaSeguridad.ipCliente(request);

        long bloqueo = intentos.segundosBloqueoRestantes(email, ip);
        if (bloqueo > 0) {
            auditoria.registrar(Evento.CUENTA_BLOQUEADA, email, request, "bloqueo activo, " + bloqueo + "s restantes");
            throw new CuentaBloqueadaException(bloqueo);
        }

        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, solicitud.password()));
        } catch (AuthenticationException e) {
            intentos.registrarFallo(email, ip);
            auditoria.registrar(Evento.LOGIN_FALLIDO, email, request, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        intentos.registrarExito(email);
        UsuarioDetails usuario = (UsuarioDetails) autenticacion.getPrincipal();
        auditoria.registrar(Evento.LOGIN_EXITOSO, email, request, "rol=" + usuario.getRol());
        String token = jwtUtil.generarToken(usuario);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", jwtUtil.getExpiracionSegundos(),
                usuario.getId(), usuario.getUsername(), usuario.getRol().name()));
    }
}

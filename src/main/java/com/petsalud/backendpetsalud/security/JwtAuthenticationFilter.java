package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que valida el JWT ANTES de que la solicitud llegue a los controladores.
 * <ol>
 *   <li>Lee la cabecera Authorization: Bearer &lt;token&gt;.</li>
 *   <li>Valida firma, emisor y expiración con {@link JwtUtil}.</li>
 *   <li>Comprueba que el usuario siga existiendo y toma su rol vigente desde la base de datos.</li>
 *   <li>Si todo es correcto, registra la autenticación en el SecurityContext (sin sesión HTTP).</li>
 * </ol>
 * Si el token es inválido responde 401 en JSON y registra el evento en el log de seguridad.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtAuthenticationEntryPoint entryPoint;
    private final AuditoriaSeguridad auditoria;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioDetailsService usuarioDetailsService,
                                   JwtAuthenticationEntryPoint entryPoint, AuditoriaSeguridad auditoria) {
        this.jwtUtil = jwtUtil;
        this.usuarioDetailsService = usuarioDetailsService;
        this.entryPoint = entryPoint;
        this.auditoria = auditoria;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            chain.doFilter(request, response); // sin token: decidirán las reglas de autorización
            return;
        }

        String token = cabecera.substring(PREFIJO.length()).trim();
        try {
            Claims claims = jwtUtil.validarToken(token);
            UsuarioDetails usuario = (UsuarioDetails) usuarioDetailsService.loadUserByUsername(claims.getSubject());

            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
        } catch (ExpiredJwtException e) {
            rechazar(request, response, e.getClaims() != null ? e.getClaims().getSubject() : null, "token expirado");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            rechazar(request, response, null, "firma/formato/emisor inválido");
            return;
        } catch (UsernameNotFoundException e) {
            rechazar(request, response, null, "usuario del token ya no existe");
            return;
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response, String usuario, String motivo)
            throws IOException {
        SecurityContextHolder.clearContext();
        auditoria.registrar(AuditoriaSeguridad.Evento.TOKEN_INVALIDO, usuario, request, motivo);
        request.setAttribute(JwtAuthenticationEntryPoint.ATRIBUTO_TOKEN_INVALIDO, Boolean.TRUE);
        entryPoint.commence(request, response, null);
    }
}

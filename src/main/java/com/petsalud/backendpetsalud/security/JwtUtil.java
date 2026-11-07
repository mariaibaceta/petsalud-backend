package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Generación y validación de tokens JWT (jjwt 0.13, API vigente).
 * <ul>
 *   <li>Firma HMAC-SHA256 con un secreto de al menos 256 bits leído desde la variable de entorno JWT_SECRET.</li>
 *   <li>Claims: sub (email), rol, iss, iat, exp y jti. Sin datos sensibles: el payload solo está codificado en Base64.</li>
 *   <li>La validación exige firma correcta, algoritmo HS256, emisor esperado y token no expirado.</li>
 * </ul>
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROL = "rol";
    private static final int MIN_BYTES_SECRETO = 32; // 256 bits, requisito de HS256 (RFC 7518 §3.2)

    private final SecretKey clave;
    private final Duration expiracion;
    private final String emisor;

    public JwtUtil(@Value("${seguridad.jwt.secreto}") String secretoBase64,
                   @Value("${seguridad.jwt.expiracion-minutos}") long expiracionMinutos,
                   @Value("${seguridad.jwt.emisor}") String emisor) {
        this.clave = construirClave(secretoBase64);
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
        this.emisor = emisor;
    }

    /** Falla al arrancar si el secreto no existe, no es Base64 o es demasiado corto (fail-fast). */
    private static SecretKey construirClave(String secretoBase64) {
        if (secretoBase64 == null || secretoBase64.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no está definido. Configure la variable de entorno (ver .env.example).");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretoBase64.trim());
        } catch (DecodingException e) {
            throw new IllegalStateException("JWT_SECRET debe estar codificado en Base64.", e);
        }
        if (bytes.length < MIN_BYTES_SECRETO) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes) para HS256.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /** Genera un token firmado para el usuario autenticado. */
    public String generarToken(UsuarioDetails usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(usuario.getUsername())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida firma, algoritmo, emisor y expiración, y devuelve los claims.
     *
     * @throws JwtException si el token es inválido, fue manipulado, expiró o pertenece a otro emisor.
     */
    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiracionSegundos() {
        return expiracion.toSeconds();
    }
}

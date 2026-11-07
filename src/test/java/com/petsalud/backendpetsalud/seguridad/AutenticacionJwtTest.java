package com.petsalud.backendpetsalud.seguridad;

import com.jayway.jsonpath.JsonPath;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Criterio 3: generación, validación y seguridad del token JWT; registro seguro (BCrypt + rol forzado). */
class AutenticacionJwtTest extends PruebaIntegracionBase {

    @Autowired
    private UsuarioRepository usuarios;

    @Value("${seguridad.jwt.secreto}")
    private String secreto;

    private String login(String email, String clave) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}";
    }

    @Test
    @DisplayName("Login correcto: 200 con token Bearer, expiración y rol")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login(ANA, CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.token", matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    @DisplayName("Claims del JWT: sub, rol, iss, iat, exp, jti y nada más (sin datos sensibles)")
    void claimsSinDatosSensibles() throws Exception {
        String[] partes = token(BRUNO).split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = JsonPath.read(payload, "$");
        assertThat(header).contains("\"alg\":\"HS256\"");
        assertThat(claims).containsOnlyKeys("sub", "rol", "iss", "iat", "exp", "jti");
        assertThat(claims.get("sub")).isEqualTo(BRUNO);
        assertThat(claims.get("rol")).isEqualTo("CLIENTE");
        assertThat(claims.get("iss")).isEqualTo("petsalud-backend");
        assertThat(((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Contraseña incorrecta y email inexistente: mismo 401 genérico (sin enumeración)")
    void credencialesInvalidas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login(ANA, "Incorrecta1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login("nadie@petsalud.cl", "Incorrecta1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Sin token: 401 JSON con WWW-Authenticate: Bearer")
    void sinToken() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Token manipulado (rol cambiado a GERENTE sin re-firmar): 401")
    void tokenManipulado() throws Exception {
        String[] p = token(ANA).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8)
                .replace("\"CLIENTE\"", "\"GERENTE\"");
        String alterado = p[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + p[2];
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"));
    }

    @Test
    @DisplayName("Token expirado (firmado con la clave correcta): 401")
    void tokenExpirado() throws Exception {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        String expirado = Jwts.builder().issuer("petsalud-backend").subject(ANA).claim("rol", "CLIENTE")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(clave, Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token firmado con otra clave (falsificado como GERENTE): 401")
    void tokenOtraFirma() throws Exception {
        SecretKey otra = Keys.hmacShaKeyFor("otra-clave-de-atacante-con-mas-de-256-bits!!".getBytes(StandardCharsets.UTF_8));
        String falso = Jwts.builder().issuer("petsalud-backend").subject(GERENTE).claim("rol", "GERENTE")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(otra, Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token de otro emisor (iss distinto, misma clave): 401")
    void tokenOtroEmisor() throws Exception {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        String otroEmisor = Jwts.builder().issuer("otro-sistema").subject(GERENTE).claim("rol", "GERENTE")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(clave, Jwts.SIG.HS256).compact();
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + otroEmisor))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token sin firma (alg=none): 401")
    void tokenSinFirma() throws Exception {
        String h = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String b = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"petsalud-backend\",\"sub\":\"" + GERENTE + "\",\"rol\":\"GERENTE\",\"exp\":4102444800}").getBytes());
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + h + "." + b + "."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registro: BCrypt, rol forzado a CLIENTE, id ignorado y sin password en la respuesta")
    void registroSeguro() throws Exception {
        String cuerpo = "{\"id\":1,\"email\":\"Camila@Cliente.cl\",\"password\":\"MiClave2026\",\"nombre\":\"Camila\",\"rol\":\"GERENTE\"}";
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.id", not(1)))
                .andExpect(jsonPath("$.email").value("camila@cliente.cl"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Usuario guardado = usuarios.findByEmail("camila@cliente.cl").orElseThrow();
        assertThat(guardado.getPassword()).startsWith("$2a$").doesNotContain("MiClave2026");
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
        Usuario gerente = usuarios.findById(1L).orElseThrow();
        assertThat(gerente.getEmail()).isEqualTo(GERENTE);
        assertThat(gerente.getRol()).isEqualTo(Rol.GERENTE);
    }

    @Test
    @DisplayName("Registro con contraseña débil o email inválido: 400")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"debil@cliente.cl\",\"password\":\"123\",\"nombre\":\"Debil\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem(startsWith("password"))));
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"Clave12345\",\"nombre\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registro con email ya existente: 400 sin crear duplicado")
    void registroDuplicado() throws Exception {
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ANA@cliente.cl\",\"password\":\"Clave12345\",\"nombre\":\"Ana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El email ya está registrado"));
    }

    @Test
    @DisplayName("Si el usuario del token fue eliminado, el token deja de servir: 401")
    void usuarioEliminadoInvalidaToken() throws Exception {
        String cuerpo = "{\"email\":\"temporal@cliente.cl\",\"password\":\"Clave12345\",\"nombre\":\"Temp\"}";
        String json = mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(json, "$.id");
        String loginJson = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("temporal@cliente.cl", "Clave12345")))
                .andReturn().getResponse().getContentAsString();
        String tokenTemporal = JsonPath.read(loginJson, "$.token");
        usuarios.deleteById(id.longValue());
        usuarios.flush();
        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + tokenTemporal))
                .andExpect(status().isUnauthorized());
    }
}

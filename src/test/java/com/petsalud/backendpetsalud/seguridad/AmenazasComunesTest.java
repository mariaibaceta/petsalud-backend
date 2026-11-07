package com.petsalud.backendpetsalud.seguridad;

import com.petsalud.backendpetsalud.repository.MascotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Criterio 5: pruebas de protección contra SQL Injection, XSS, CSRF y otras amenazas comunes. */
class AmenazasComunesTest extends PruebaIntegracionBase {

    @Autowired
    private MascotaRepository mascotas;

    private static RequestPostProcessor desdeIp(String ip) {
        return r -> {
            r.setRemoteAddr(ip);
            return r;
        };
    }

    @Nested
    @DisplayName("SQL Injection")
    class SqlInjection {

        @ParameterizedTest(name = "email = {0}")
        @ValueSource(strings = {"' OR '1'='1' --", "admin'--", "\" OR 1=1 #", "x' UNION SELECT * FROM usuarios --"})
        @DisplayName("Payloads en el email del login: 400 por validación, sin token ni error SQL")
        void enEmailDeLogin(String payload) throws Exception {
            mvc.perform(post("/api/auth/login").with(desdeIp("10.1.1.1")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + payload.replace("\"", "\\\"") + "\",\"password\":\"x\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.token").doesNotExist())
                    .andExpect(content().string(not(containsStringIgnoringCase("sql"))));
        }

        @Test
        @DisplayName("Payload en la contraseña con email válido: 401 (sin bypass de autenticación)")
        void enPasswordDeLogin() throws Exception {
            mvc.perform(post("/api/auth/login").with(desdeIp("10.1.1.2")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + ANA + "\",\"password\":\"' OR '1'='1\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "categoria = {0}")
        @ValueSource(strings = {"x' OR '1'='1", "Juguetes' --", "x'; DROP TABLE productos; --", "' OR 1=1 #"})
        @DisplayName("Payloads en /productos/categoria: lista vacía (consulta parametrizada) y la tabla sigue intacta")
        void enCategoria(String payload) throws Exception {
            mvc.perform(get("/api/productos/categoria").param("categoria", payload).header("Authorization", bearer(ANA)))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
            mvc.perform(get("/api/productos").header("Authorization", bearer(ANA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        }

        @Test
        @DisplayName("UNION SELECT largo en categoría: 400 por validación de tamaño, sin filtrar datos")
        void unionSelect() throws Exception {
            mvc.perform(get("/api/productos/categoria")
                            .param("categoria", "x' UNION SELECT email,password,1,1,1,1 FROM usuarios -- ")
                            .header("Authorization", bearer(ANA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(not(containsString("$2a$"))));
        }

        @ParameterizedTest(name = "id = {0}")
        @ValueSource(strings = {"1 OR 1=1", "1;DROP TABLE mascotas", "1'"})
        @DisplayName("Payloads en ids de ruta: 400 (el id debe ser numérico)")
        void enIdDeRuta(String payload) throws Exception {
            mvc.perform(get("/api/mascotas/" + payload).header("Authorization", bearer(EMPLEADO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(not(containsStringIgnoringCase("sql"))));
        }
    }

    @Nested
    @DisplayName("XSS (Cross-Site Scripting)")
    class Xss {

        @ParameterizedTest(name = "nombre = {0}")
        @ValueSource(strings = {
                "<script>alert('XSS')</script>",
                "<img src=x onerror=alert(1)>",
                "<svg/onload=alert(1)>",
                "javascript:alert(document.cookie)",
                "<a href=\"javascript:alert(1)\">x</a>",
                "Luna\" onmouseover=\"alert(1)"})
        @DisplayName("Payloads XSS en texto libre: 400 y no se persisten")
        void payloadsRechazados(String payload) throws Exception {
            long antes = mascotas.count();
            String json = "{\"nombre\":\"" + payload.replace("\"", "\\\"") + "\",\"especie\":\"Perro\"}";
            mvc.perform(post("/api/mascotas/propietario/3").header("Authorization", bearer(ANA))
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles", hasItem(startsWith("nombre"))));
            assertThat(mascotas.count()).isEqualTo(antes);
        }

        @Test
        @DisplayName("XSS en notificación (empleado) y en motivo de cita (request param): 400")
        void otrosCampos() throws Exception {
            mvc.perform(post("/api/notificaciones/usuario/3").header("Authorization", bearer(EMPLEADO))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mensaje\":\"<img src=x onerror=alert(1)>\",\"tipo\":\"Promoción\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/api/citas").header("Authorization", bearer(ANA))
                            .param("mascotaId", "1").param("veterinarioId", "2")
                            .param("fechaHora", "2030-02-01T10:00:00").param("motivo", "<script>alert(1)</script>"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Texto legítimo con tildes, ñ, <, & y comillas se acepta sin alterarse")
        void sinFalsosPositivos() throws Exception {
            mvc.perform(post("/api/mascotas/propietario/3").header("Authorization", bearer(ANA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Ñandú O'Higgins\",\"especie\":\"Perro\",\"raza\":\"Mestizo (5 < 6 años) & juguetón\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Ñandú O'Higgins"))
                    .andExpect(jsonPath("$.raza").value("Mestizo (5 < 6 años) & juguetón"));
        }

        @Test
        @DisplayName("Cabeceras de seguridad: CSP, nosniff, X-Frame-Options, Cache-Control y JSON")
        void cabecerasDeSeguridad() throws Exception {
            mvc.perform(get("/api/productos").header("Authorization", bearer(ANA)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(header().string("Referrer-Policy", "no-referrer"));
        }
    }

    @Nested
    @DisplayName("CSRF y CORS")
    class CsrfCors {

        @Test
        @DisplayName("Petición que cambia estado solo con cookies (sin Authorization): 401")
        void cookiesNoAutentican() throws Exception {
            mvc.perform(delete("/api/notificaciones/1")
                            .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "ABC123"),
                                    new jakarta.servlet.http.Cookie("XSRF-TOKEN", "abc")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Stateless: el login y las peticiones autenticadas no crean sesión ni JSESSIONID")
        void sinSesion() throws Exception {
            var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + BRUNO + "\",\"password\":\"" + CLAVE + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"))
                    .andReturn();
            assertThat(login.getRequest().getSession(false)).isNull();
            var consulta = mvc.perform(get("/api/productos").header("Authorization", bearer(BRUNO)))
                    .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
            assertThat(consulta.getRequest().getSession(false)).isNull();
        }

        @Test
        @DisplayName("CORS: preflight desde origen no permitido → 403; desde origen permitido → 200")
        void cors() throws Exception {
            mvc.perform(options("/api/productos").header("Origin", "https://sitio-malicioso.example")
                            .header("Access-Control-Request-Method", "DELETE"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
            mvc.perform(options("/api/productos").header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }

        @Test
        @DisplayName("Petición de otro sitio con cookies y Origin malicioso: rechazada (403 CORS)")
        void origenCruzadoConCookies() throws Exception {
            mvc.perform(delete("/api/notificaciones/1").header("Origin", "https://sitio-malicioso.example")
                            .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "ABC123")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Exposición de datos, errores y asignación masiva")
    class ExposicionYErrores {

        @Test
        @DisplayName("Ninguna respuesta de usuarios incluye password ni relaciones")
        void sinPassword() throws Exception {
            mvc.perform(get("/api/usuarios").header("Authorization", bearer(GERENTE)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("password"))))
                    .andExpect(content().string(not(containsString("$2a$"))))
                    .andExpect(content().string(not(containsString("mascotas"))));
        }

        @Test
        @DisplayName("Error de formato: 400 JSON sin stack trace ni nombres de clases")
        void sinStackTrace() throws Exception {
            mvc.perform(get("/api/citas/entre-fechas").param("inicio", "abc").param("fin", "xyz")
                            .header("Authorization", bearer(EMPLEADO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.trace").doesNotExist())
                    .andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("java."))));
        }

        @Test
        @DisplayName("JSON malformado: 400 genérico")
        void jsonMalformado() throws Exception {
            mvc.perform(post("/api/productos").header("Authorization", bearer(EMPLEADO))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es un JSON válido"));
        }

        @Test
        @DisplayName("Actualizar usuario con rol/email/password en el body no los modifica")
        void asignacionMasivaEnActualizacion() throws Exception {
            mvc.perform(put("/api/usuarios/3").header("Authorization", bearer(ANA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Ana\",\"rol\":\"GERENTE\",\"email\":\"otra@evil.cl\",\"password\":\"x\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rol").value("CLIENTE"))
                    .andExpect(jsonPath("$.email").value(ANA));
        }

        @Test
        @DisplayName("Un gerente no puede quitarse su propio rol ni eliminarse a sí mismo")
        void gerenteNoSeAutoModifica() throws Exception {
            mvc.perform(put("/api/usuarios/1/rol").header("Authorization", bearer(GERENTE))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"CLIENTE\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(delete("/api/usuarios/1").header("Authorization", bearer(GERENTE)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("B16: solo un EMPLEADO puede figurar como veterinario de una cita o expediente")
        void veterinarioDebeSerEmpleado() throws Exception {
            mvc.perform(post("/api/citas").header("Authorization", bearer(ANA))
                            .param("mascotaId", "1").param("veterinarioId", "4")
                            .param("fechaHora", "2030-02-01T10:00:00").param("motivo", "Control"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje", containsString("no es un empleado")));
            mvc.perform(post("/api/expedientes").header("Authorization", bearer(EMPLEADO))
                            .param("mascotaId", "1").param("veterinarioId", "3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descripcion\":\"x\",\"diagnostico\":\"y\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Fuerza bruta")
    class FuerzaBruta {

        @Test
        @DisplayName("5 fallos para un email → 6.º intento bloqueado (429 + Retry-After) aunque la clave sea correcta")
        void bloqueoPorEmail() throws Exception {
            String json = "{\"email\":\"" + BRUNO + "\",\"password\":\"%s\"}";
            for (int i = 1; i <= 5; i++) {
                mvc.perform(post("/api/auth/login").with(desdeIp("10.2.2.2")).contentType(MediaType.APPLICATION_JSON)
                                .content(json.formatted("Adivinanza" + i)))
                        .andExpect(status().isUnauthorized());
            }
            mvc.perform(post("/api/auth/login").with(desdeIp("10.2.2.3")).contentType(MediaType.APPLICATION_JSON)
                            .content(json.formatted(CLAVE)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Password spraying: 20 fallos desde una IP contra emails distintos → la IP queda bloqueada")
        void bloqueoPorIp() throws Exception {
            for (int i = 1; i <= 20; i++) {
                mvc.perform(post("/api/auth/login").with(desdeIp("10.3.3.3")).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"victima" + i + "@cliente.cl\",\"password\":\"Verano2026\"}"))
                        .andExpect(status().isUnauthorized());
            }
            mvc.perform(post("/api/auth/login").with(desdeIp("10.3.3.3")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMPLEADO + "\",\"password\":\"" + CLAVE + "\"}"))
                    .andExpect(status().isTooManyRequests());
            // Desde otra IP el empleado sigue pudiendo entrar (no se bloquea a toda la clínica)
            mvc.perform(post("/api/auth/login").with(desdeIp("10.3.3.4")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + EMPLEADO + "\",\"password\":\"" + CLAVE + "\"}"))
                    .andExpect(status().isOk());
        }
    }
}

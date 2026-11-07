package com.petsalud.backendpetsalud.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Criterio 4: TODOS los endpoints tienen regla de acceso y se comportan según la matriz de roles.
 * <p>
 * Cada fila se ejecuta con los 4 usuarios (G = gerente, E = empleado, A = Ana, B = Bruno) y sin token.
 * <ul>
 *   <li>Usuario permitido: respuesta 2xx (sin 401/403 y sin errores 4xx/5xx).</li>
 *   <li>Usuario no permitido: 403 Forbidden.</li>
 *   <li>Sin token: 401 Unauthorized.</li>
 * </ul>
 * Las filas "propio" usan recursos de Ana o de Bruno, por lo que también verifican IDOR en ambos sentidos.
 */
class MatrizAutorizacionTest extends PruebaIntegracionBase {

    private static final String MASCOTA = "{\"nombre\":\"Toby\",\"especie\":\"Perro\",\"raza\":\"Beagle\",\"sexo\":\"M\"}";
    private static final String USUARIO = "{\"nombre\":\"Nombre\",\"apellido\":\"Apellido\",\"telefono\":\"+56911112222\",\"direccion\":\"Santiago\"}";
    private static final String EXPEDIENTE = "{\"descripcion\":\"Control\",\"diagnostico\":\"Sano\",\"tratamiento\":\"Ninguno\"}";
    private static final String PRODUCTO = "{\"nombre\":\"Collar reflectante\",\"descripcion\":\"Talla M\",\"precio\":5990,\"stock\":10,\"categoria\":\"Accesorios\"}";
    private static final String RECOMPENSA = "{\"puntos\":100,\"descripcion\":\"Bono de bienvenida\"}";
    private static final String NOTIFICACION = "{\"mensaje\":\"Jornada de adopción el sábado\",\"tipo\":\"Evento\"}";

    record Caso(String metodo, String uri, String cuerpo, Set<Character> permitidos) {
        @Override
        public String toString() {
            return metodo + " " + uri + " permitido=" + permitidos;
        }
    }

    private static Caso c(String metodo, String uri, String cuerpo, String permitidos) {
        Set<Character> roles = new java.util.HashSet<>();
        for (char r : permitidos.toCharArray()) {
            roles.add(r);
        }
        return new Caso(metodo, uri, cuerpo, roles);
    }

    /** Matriz de la sección 7.3 (A = recurso de Ana, B = recurso de Bruno). */
    static List<Caso> matriz() {
        String futuro = "2030-01-15T10:00:00";
        return List.of(
                // ---------- Usuarios ----------
                c("GET", "/api/usuarios/3", null, "GEA"),
                c("GET", "/api/usuarios/4", null, "GEB"),
                c("PUT", "/api/usuarios/3", USUARIO, "GA"),
                c("PUT", "/api/usuarios/2", USUARIO, "GE"),
                c("PUT", "/api/usuarios/4/rol", "{\"rol\":\"EMPLEADO\"}", "G"),
                c("DELETE", "/api/usuarios/4", null, "G"),
                c("GET", "/api/usuarios", null, "GE"),
                // ---------- Mascotas ----------
                c("POST", "/api/mascotas/propietario/3", MASCOTA, "GEA"),
                c("POST", "/api/mascotas/propietario/4", MASCOTA, "GEB"),
                c("GET", "/api/mascotas/1", null, "GEA"),
                c("GET", "/api/mascotas/2", null, "GEB"),
                c("PUT", "/api/mascotas/1", MASCOTA, "GEA"),
                c("PUT", "/api/mascotas/2", MASCOTA, "GEB"),
                c("DELETE", "/api/mascotas/1", null, "GA"),
                c("DELETE", "/api/mascotas/2", null, "GB"),
                c("GET", "/api/mascotas/propietario/3", null, "GEA"),
                c("GET", "/api/mascotas/propietario/4", null, "GEB"),
                // ---------- Citas ----------
                c("POST", "/api/citas?mascotaId=1&veterinarioId=2&fechaHora=" + futuro + "&motivo=Control", null, "GEA"),
                c("POST", "/api/citas?mascotaId=2&veterinarioId=2&fechaHora=" + futuro + "&motivo=Control", null, "GEB"),
                c("GET", "/api/citas/1", null, "GEA"),
                c("GET", "/api/citas/2", null, "GEB"),
                c("PUT", "/api/citas/1/estado?estado=Confirmada", null, "GE"),
                c("DELETE", "/api/citas/1", null, "GEA"),
                c("DELETE", "/api/citas/2", null, "GEB"),
                c("GET", "/api/citas/mascota/1", null, "GEA"),
                c("GET", "/api/citas/mascota/2", null, "GEB"),
                c("GET", "/api/citas/veterinario/2", null, "GE"),
                c("GET", "/api/citas/entre-fechas?inicio=2020-01-01T00:00:00&fin=2035-01-01T00:00:00", null, "GE"),
                // ---------- Expedientes médicos ----------
                c("POST", "/api/expedientes?mascotaId=1&veterinarioId=2", EXPEDIENTE, "GE"),
                c("GET", "/api/expedientes/1", null, "GEA"),
                c("GET", "/api/expedientes/2", null, "GEB"),
                c("GET", "/api/expedientes/mascota/1", null, "GEA"),
                c("GET", "/api/expedientes/mascota/2", null, "GEB"),
                c("DELETE", "/api/expedientes/1", null, "G"),
                // ---------- Productos ----------
                c("POST", "/api/productos", PRODUCTO, "GE"),
                c("GET", "/api/productos/1", null, "GEAB"),
                c("PUT", "/api/productos/1", PRODUCTO, "GE"),
                c("DELETE", "/api/productos/3", null, "G"),
                c("GET", "/api/productos", null, "GEAB"),
                c("GET", "/api/productos/categoria?categoria=Juguetes", null, "GEAB"),
                c("GET", "/api/productos/stock-bajo?cantidad=10", null, "GE"),
                // ---------- Ventas ----------
                c("POST", "/api/ventas?usuarioId=3", "[2]", "GEA"),
                c("POST", "/api/ventas?usuarioId=4", "[2]", "GEB"),
                c("GET", "/api/ventas/1", null, "GEB"),
                c("GET", "/api/ventas/usuario/3", null, "GEA"),
                c("GET", "/api/ventas/usuario/4", null, "GEB"),
                c("GET", "/api/ventas", null, "G"),
                // ---------- Recompensas ----------
                c("POST", "/api/recompensas/usuario/4", RECOMPENSA, "GE"),
                c("GET", "/api/recompensas/1", null, "GEB"),
                c("GET", "/api/recompensas/usuario/4", null, "GEB"),
                c("DELETE", "/api/recompensas/1", null, "G"),
                // ---------- Notificaciones ----------
                c("POST", "/api/notificaciones/usuario/3", NOTIFICACION, "GE"),
                c("GET", "/api/notificaciones/1", null, "GEA"),
                c("GET", "/api/notificaciones/2", null, "GEB"),
                c("GET", "/api/notificaciones/usuario/3", null, "GEA"),
                c("GET", "/api/notificaciones/usuario/4/no-leidas", null, "GEB"),
                c("PUT", "/api/notificaciones/1/leida", null, "GA"),
                c("DELETE", "/api/notificaciones/2", null, "GB")
        );
    }

    static Stream<Arguments> casosPorRol() {
        List<Arguments> args = new ArrayList<>();
        for (Caso caso : matriz()) {
            args.add(Arguments.of('G', GERENTE, caso));
            args.add(Arguments.of('E', EMPLEADO, caso));
            args.add(Arguments.of('A', ANA, caso));
            args.add(Arguments.of('B', BRUNO, caso));
        }
        return args.stream();
    }

    static Stream<Caso> casos() {
        return matriz().stream();
    }

    @ParameterizedTest(name = "[{0}] {2}")
    @MethodSource("casosPorRol")
    @DisplayName("Cada rol obtiene 2xx si está permitido y 403 si no")
    void cadaRolRecibeLaRespuestaEsperada(char rol, String email, Caso caso) throws Exception {
        int status = mvc.perform(construir(caso).header("Authorization", bearer(email)))
                .andReturn().getResponse().getStatus();
        if (caso.permitidos().contains(rol)) {
            assertThat(status).as("%s debería estar PERMITIDO para %s", caso, email).isBetween(200, 299);
        } else {
            assertThat(status).as("%s debería estar DENEGADO para %s", caso, email).isEqualTo(403);
        }
    }

    @ParameterizedTest(name = "sin token: {0}")
    @MethodSource("casos")
    @DisplayName("Ningún endpoint protegido responde sin JWT (401)")
    void sinTokenSiempre401(Caso caso) throws Exception {
        int status = mvc.perform(construir(caso)).andReturn().getResponse().getStatus();
        assertThat(status).as("%s sin token", caso).isEqualTo(401);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("La matriz cubre los 41 endpoints protegidos originales + el endpoint de cambio de rol")
    void laMatrizCubreTodosLosEndpoints() {
        long distintos = matriz().stream()
                .map(c -> c.metodo() + " " + c.uri()
                        .replaceAll("\\?.*", "")
                        .replaceAll("/\\d+", "/{id}"))
                .distinct().count();
        // 42 originales = 41 protegidos + POST /registro (público). Se suma PUT /api/usuarios/{id}/rol.
        // Los públicos (registro y login) se prueban en AutenticacionJwtTest.
        assertThat(distintos).isEqualTo(42);
    }

    private static MockHttpServletRequestBuilder construir(Caso caso) {
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(caso.metodo()), caso.uri());
        if (caso.cuerpo() != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(caso.cuerpo());
        }
        return req;
    }
}

package com.petsalud.backendpetsalud.seguridad;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base de las pruebas de integración: contexto completo, MockMvc real (con todos los filtros de seguridad),
 * H2 en memoria y datos de demostración del perfil test. Cada prueba corre en una transacción que se revierte.
 * <p>
 * Datos sembrados (ids deterministas): 1 gerente, 2 empleado, 3 Ana (cliente), 4 Bruno (cliente);
 * mascota 1 Luna (Ana), mascota 2 Michi (Bruno); expediente/cita 1 de Luna y 2 de Michi;
 * venta 1 y recompensa 1 de Bruno; notificación 1 de Ana y 2 de Bruno.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class PruebaIntegracionBase {

    protected static final String CLAVE = "PetSalud#2026";
    public static final String GERENTE = "gerente@petsalud.cl";
    public static final String EMPLEADO = "empleado@petsalud.cl";
    public static final String ANA = "ana@cliente.cl";
    public static final String BRUNO = "bruno@cliente.cl";

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired
    protected MockMvc mvc;

    /** Obtiene (y reutiliza) un JWT real mediante el endpoint de login. */
    protected String token(String email) throws Exception {
        String cache = TOKENS.get(email);
        if (cache != null) {
            return cache;
        }
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + CLAVE + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.token");
        TOKENS.put(email, token);
        return token;
    }

    protected String bearer(String email) throws Exception {
        return "Bearer " + token(email);
    }
}

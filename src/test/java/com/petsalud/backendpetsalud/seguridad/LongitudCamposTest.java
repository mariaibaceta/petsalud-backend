package com.petsalud.backendpetsalud.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El largo de cada columna coincide con el @Size del DTO: un texto del largo máximo permitido se guarda
 * (antes, con VARCHAR(255), terminaba en 409) y uno que lo supera se rechaza con 400 antes de llegar a la BD.
 */
class LongitudCamposTest extends PruebaIntegracionBase {

    private static String texto(int largo) {
        return "a".repeat(largo);
    }

    private static String expediente(int largo) {
        return "{\"descripcion\":\"%s\",\"diagnostico\":\"%s\",\"tratamiento\":\"%s\"}"
                .formatted(texto(largo), texto(largo), texto(largo));
    }

    private static String notificacion(int largo) {
        return "{\"mensaje\":\"%s\",\"tipo\":\"Recordatorio\"}".formatted(texto(largo));
    }

    private static String producto(int largo) {
        return "{\"nombre\":\"Arnés\",\"descripcion\":\"%s\",\"precio\":9990,\"stock\":5,\"categoria\":\"Accesorios\"}"
                .formatted(texto(largo));
    }

    @Test
    @DisplayName("Textos del largo máximo permitido (300 y 500 caracteres) se guardan completos: 201")
    void largoMaximoSeGuarda() throws Exception {
        mvc.perform(post("/api/expedientes").header("Authorization", bearer(EMPLEADO))
                        .param("mascotaId", "1").param("veterinarioId", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(expediente(500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion", hasLength(500)))
                .andExpect(jsonPath("$.diagnostico", hasLength(500)))
                .andExpect(jsonPath("$.tratamiento", hasLength(500)));
        mvc.perform(post("/api/citas").header("Authorization", bearer(ANA))
                        .param("mascotaId", "1").param("veterinarioId", "2")
                        .param("fechaHora", "2030-03-01T10:00:00").param("motivo", texto(300)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo", hasLength(300)));
        mvc.perform(post("/api/notificaciones/usuario/3").header("Authorization", bearer(EMPLEADO))
                        .contentType(MediaType.APPLICATION_JSON).content(notificacion(300)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje", hasLength(300)));
        mvc.perform(post("/api/productos").header("Authorization", bearer(EMPLEADO))
                        .contentType(MediaType.APPLICATION_JSON).content(producto(300)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion", hasLength(300)));
    }

    @Test
    @DisplayName("Textos que superan el máximo (301 y 501 caracteres) se rechazan por validación: 400")
    void sobreElMaximoResponde400() throws Exception {
        mvc.perform(post("/api/expedientes").header("Authorization", bearer(EMPLEADO))
                        .param("mascotaId", "1").param("veterinarioId", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(expediente(501)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/citas").header("Authorization", bearer(ANA))
                        .param("mascotaId", "1").param("veterinarioId", "2")
                        .param("fechaHora", "2030-03-01T10:00:00").param("motivo", texto(301)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notificaciones/usuario/3").header("Authorization", bearer(EMPLEADO))
                        .contentType(MediaType.APPLICATION_JSON).content(notificacion(301)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos").header("Authorization", bearer(EMPLEADO))
                        .contentType(MediaType.APPLICATION_JSON).content(producto(301)))
                .andExpect(status().isBadRequest());
    }
}

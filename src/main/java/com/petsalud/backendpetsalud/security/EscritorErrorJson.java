package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Escribe respuestas de error JSON desde la cadena de filtros (fuera de los controladores). */
@Component
public class EscritorErrorJson {

    private final JsonMapper jsonMapper;

    public EscritorErrorJson(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void escribir(HttpServletResponse response, ErrorRespuesta error) throws IOException {
        response.setStatus(error.status());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(error));
    }
}

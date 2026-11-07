package com.petsalud.backendpetsalud.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Paso 1 del formato: monitoreo básico de actividades sospechosas en el log de seguridad. */
@ExtendWith(OutputCaptureExtension.class)
class MonitoreoSeguridadTest extends PruebaIntegracionBase {

    @Test
    @DisplayName("Login fallido, acceso denegado, token inválido y sin token quedan registrados sin datos sensibles")
    void eventosRegistrados(CapturedOutput salida) throws Exception {
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("10.9.9.9"); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@cliente.cl\",\"password\":\"ClaveSecretaIncorrecta9\"}"));
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(ANA)));
        mvc.perform(get("/api/productos").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.firma-falsa"));
        mvc.perform(get("/api/productos"));

        String log = salida.getAll();
        assertThat(log).contains("evento=LOGIN_FALLIDO usuario=an***@cliente.cl ip=10.9.9.9");
        assertThat(log).contains("evento=ACCESO_DENEGADO usuario=an***@cliente.cl");
        assertThat(log).contains("evento=TOKEN_INVALIDO");
        assertThat(log).contains("evento=ACCESO_NO_AUTENTICADO");
        // Nunca se registran contraseñas, tokens ni el email completo
        assertThat(log).doesNotContain("ClaveSecretaIncorrecta9");
        assertThat(log).doesNotContain("firma-falsa");
        assertThat(log).doesNotContain("usuario=ana@cliente.cl");
    }

    @Test
    @DisplayName("Intento de inyección en el log (CR/LF en el email) queda neutralizado")
    void inyeccionEnLog(CapturedOutput salida) throws Exception {
        mvc.perform(post("/api/auth/login").with(r -> { r.setRemoteAddr("10.9.9.8"); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@y.cl\\nevento=LOGIN_EXITOSO usuario=gerente\",\"password\":\"x\"}"));
        assertThat(salida.getAll()).doesNotContain("\nevento=LOGIN_EXITOSO usuario=gerente");
    }
}

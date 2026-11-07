package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.validation.TextoSeguroValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria del validador anti-XSS (OWASP Java HTML Sanitizer). */
class TextoSeguroValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>", "<img src=x onerror=alert(1)>", "<svg onload=alert(1)>",
            "<iframe src=//evil.example>", "javascript:alert(1)", "JaVaScRiPt :alert(1)", "vbscript:msgbox(1)",
            "data:text/html;base64,PHNjcmlwdD4=", "x\" onfocus=\"alert(1)", "<b>negrita</b>", "<!-- comentario -->"})
    void detectaContenidoActivo(String valor) {
        assertThat(TextoSeguroValidator.contieneContenidoActivo(valor)).as(valor).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Luna", "Ñandú O'Higgins", "Mestizo (5 < 6 años) & juguetón", "Otitis externa; gotas 2/día",
            "Condición=estable", "Vacuna \"óctuple\"", "Precio: $45.990", "email@dominio.cl", "Control 10:30 hrs."})
    void aceptaTextoLegitimo(String valor) {
        assertThat(TextoSeguroValidator.contieneContenidoActivo(valor)).as(valor).isFalse();
    }
}

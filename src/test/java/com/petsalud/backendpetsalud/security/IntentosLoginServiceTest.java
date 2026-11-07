package com.petsalud.backendpetsalud.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria del bloqueo temporal con reloj controlado. */
class IntentosLoginServiceTest {

    private static class RelojMovible extends Clock {
        private Instant ahora = Instant.parse("2026-11-06T12:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    @DisplayName("Bloquea al 5.º fallo por email y libera al cumplirse los 15 minutos")
    void bloqueoYExpiracion() {
        RelojMovible reloj = new RelojMovible();
        IntentosLoginService servicio = new IntentosLoginService(5, 20, Duration.ofMinutes(15), reloj);
        for (int i = 0; i < 4; i++) {
            servicio.registrarFallo("ana@cliente.cl", "1.1.1.1");
        }
        assertThat(servicio.segundosBloqueoRestantes("ana@cliente.cl", "2.2.2.2")).isZero();
        servicio.registrarFallo("ANA@cliente.cl", "1.1.1.1");
        assertThat(servicio.segundosBloqueoRestantes("ana@cliente.cl", "2.2.2.2")).isEqualTo(900);
        reloj.avanzar(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(servicio.segundosBloqueoRestantes("ana@cliente.cl", "2.2.2.2")).isZero();
    }

    @Test
    @DisplayName("Un login exitoso reinicia el contador del email pero NO el de la IP")
    void exitoNoLimpiaIp() {
        IntentosLoginService servicio = new IntentosLoginService(5, 3, Duration.ofMinutes(15), new RelojMovible());
        servicio.registrarFallo("a@x.cl", "9.9.9.9");
        servicio.registrarFallo("b@x.cl", "9.9.9.9");
        servicio.registrarExito("propio@x.cl");
        servicio.registrarFallo("c@x.cl", "9.9.9.9");
        assertThat(servicio.segundosBloqueoRestantes("otro@x.cl", "9.9.9.9")).isPositive();
    }
}

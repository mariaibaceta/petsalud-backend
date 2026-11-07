package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mitigación de fuerza bruta: bloqueo temporal tras N intentos fallidos de login,
 * contabilizados por email y por IP de origen (ventana = duración del bloqueo).
 * <ul>
 *   <li>Por email (umbral bajo): protege cada cuenta frente a adivinación de contraseña.</li>
 *   <li>Por IP (umbral mayor): frena el "password spraying" contra muchas cuentas sin bloquear
 *       a toda una sucursal que comparte IP pública (NAT).</li>
 *   <li>Un login exitoso solo reinicia el contador del email: así un atacante no puede "limpiar"
 *       el contador de su IP intercalando inicios de sesión válidos.</li>
 * </ul>
 * <p>
 * Implementación en memoria, adecuada para una instancia. En un despliegue con varias instancias
 * (4 sucursales escalando horizontalmente) el contador debe moverse a un almacén compartido (p. ej. Redis).
 */
@Service
public class IntentosLoginService {

    private record Registro(int fallos, Instant primerFallo, Instant bloqueadoHasta) { }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maxIntentosEmail;
    private final int maxIntentosIp;
    private final Duration bloqueo;
    private final Clock reloj;

    @Autowired
    public IntentosLoginService(@Value("${seguridad.login.max-intentos}") int maxIntentosEmail,
                                @Value("${seguridad.login.max-intentos-ip}") int maxIntentosIp,
                                @Value("${seguridad.login.bloqueo-minutos}") long bloqueoMinutos) {
        this(maxIntentosEmail, maxIntentosIp, Duration.ofMinutes(bloqueoMinutos), Clock.systemUTC());
    }

    IntentosLoginService(int maxIntentosEmail, int maxIntentosIp, Duration bloqueo, Clock reloj) {
        this.maxIntentosEmail = maxIntentosEmail;
        this.maxIntentosIp = maxIntentosIp;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    /** Segundos restantes de bloqueo para el email o la IP; 0 si no está bloqueado. */
    public long segundosBloqueoRestantes(String email, String ip) {
        return Math.max(restantes(claveEmail(email)), restantes(claveIp(ip)));
    }

    public void registrarFallo(String email, String ip) {
        fallo(claveEmail(email), maxIntentosEmail);
        fallo(claveIp(ip), maxIntentosIp);
    }

    /** Solo reinicia el contador del email (el de la IP expira por tiempo). */
    public void registrarExito(String email) {
        registros.remove(claveEmail(email));
    }

    private long restantes(String clave) {
        Registro r = registros.get(clave);
        if (r == null || r.bloqueadoHasta() == null) {
            return 0;
        }
        long seg = Duration.between(reloj.instant(), r.bloqueadoHasta()).toSeconds();
        if (seg <= 0) {
            registros.remove(clave);
            return 0;
        }
        return seg;
    }

    private void fallo(String clave, int maxIntentos) {
        Instant ahora = reloj.instant();
        registros.compute(clave, (k, r) -> {
            if (r == null || r.primerFallo().plus(bloqueo).isBefore(ahora)) {
                r = new Registro(0, ahora, null);
            }
            int fallos = r.fallos() + 1;
            Instant hasta = fallos >= maxIntentos ? ahora.plus(bloqueo) : r.bloqueadoHasta();
            return new Registro(fallos, r.primerFallo(), hasta);
        });
    }

    private static String claveEmail(String email) {
        return "email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String claveIp(String ip) {
        return "ip:" + (ip == null ? "" : ip);
    }
}

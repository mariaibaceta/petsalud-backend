package com.petsalud.backendpetsalud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.owasp.html.Encoding;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.regex.Pattern;

/**
 * Detecta contenido activo usando la política más restrictiva de OWASP Java HTML Sanitizer
 * (no se permite ninguna etiqueta ni atributo). Si al sanear el texto este cambia, significa
 * que contenía marcado HTML y la entrada se rechaza con 400 en lugar de persistirse.
 * <p>
 * Se rechaza (en vez de sanear silenciosamente) para no alterar datos clínicos sin aviso.
 */
public class TextoSeguroValidator implements ConstraintValidator<TextoSeguro, String> {

    /** Política OWASP sin elementos permitidos: elimina toda etiqueta, atributo y script. */
    private static final PolicyFactory SIN_HTML = new HtmlPolicyBuilder().toFactory();

    /** Esquemas peligrosos y manejadores de eventos aunque no vengan dentro de etiquetas. */
    private static final Pattern PATRON_PELIGROSO =
            Pattern.compile("(?i)(javascript\\s*:|vbscript\\s*:|data\\s*:\\s*text/html|\\bon[a-z]+\\s*=)");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isEmpty()) {
            return true; // la obligatoriedad la controla @NotBlank
        }
        return !contieneContenidoActivo(valor);
    }

    public static boolean contieneContenidoActivo(String valor) {
        if (PATRON_PELIGROSO.matcher(valor).find()) {
            return true;
        }
        String saneado = Encoding.decodeHtml(SIN_HTML.sanitize(valor));
        return !saneado.equals(valor);
    }
}

package com.petsalud.backendpetsalud.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El texto no puede contener HTML, scripts ni URLs "javascript:" (prevención de XSS almacenado).
 * Validado con OWASP Java HTML Sanitizer en {@link TextoSeguroValidator}.
 */
@Documented
@Constraint(validatedBy = TextoSeguroValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TextoSeguro {
    String message() default "contiene HTML o código no permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

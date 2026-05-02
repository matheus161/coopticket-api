package br.com.coopticket.usuario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Senha inválida. Deve conter pelo menos 8 caracteres, incluindo letra maiúscula, minúscula e número.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

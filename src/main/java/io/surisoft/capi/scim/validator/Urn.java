package io.surisoft.capi.scim.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
@Constraint(validatedBy = UrnValidator.class)
@Target( { TYPE_USE, METHOD, FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Urn {
  String message() default "The urn is malformed";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
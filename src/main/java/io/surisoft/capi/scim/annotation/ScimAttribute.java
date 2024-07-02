package io.surisoft.capi.scim.annotation;

import io.surisoft.capi.scim.schema.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ScimAttribute {
  
  //This is an ugly but necessary work around
  //We need something to determine which canonical value
  //parameter is desired and we can't use null so we had
  //to create this little empty enum as a place holder
  //for assignment checks.
  enum NoOp {
  }
  
  String name() default "";
  boolean required() default false;
  
  //These two canonical attributes should be mutually exclusive, if both are 
  //present we will reject the registered repository
  Class<? extends Enum<?>>  canonicalValueEnum() default NoOp.class;
  String [] canonicalValueList() default "";
  
  boolean caseExact() default false;
  Schema.Attribute.Mutability mutability() default Schema.Attribute.Mutability.READ_WRITE;
  Schema.Attribute.Returned returned() default Schema.Attribute.Returned.DEFAULT;
  Schema.Attribute.Uniqueness uniqueness() default Schema.Attribute.Uniqueness.NONE;
  String [] referenceTypes() default "";
  String description() default "";
}

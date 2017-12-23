package org.klesun.lang;

import java.lang.annotation.*;

/**
 * you have to define your own NonNull annotation to be able to mark a generic as not null
 * because IDEA does not allow using it's @NonNull for java 7 compatibility reasons
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(value=ElementType.TYPE_USE) // this is the parameter that was introduced in java 8
public @interface NonNull {
    String value() default "";

    Class<? extends Exception> exception() default Exception.class;
}

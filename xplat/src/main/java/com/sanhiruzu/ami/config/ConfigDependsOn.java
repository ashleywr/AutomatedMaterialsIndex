package com.sanhiruzu.ami.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a config value depends on another field being true (or a specific value).
 * If the dependency is not met, the field will be disabled in the UI.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDependsOn {
    String value();

    String expected() default "true";
}

package com.hymnsmobile.pipeline.russian.dagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Scope;

/**
 * Objects will last the lifetime of Russian step of the pipeline.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface RussianScope {
}

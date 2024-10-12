package com.hymnsmobile.pipeline.h4a.dagger;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for list of songs that require special one-off attention.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface OneOff {
}

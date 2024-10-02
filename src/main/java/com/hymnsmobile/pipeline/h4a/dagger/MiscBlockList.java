package com.hymnsmobile.pipeline.h4a.dagger;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for songs that are blocked for miscellaneous reasons.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface MiscBlockList {
}

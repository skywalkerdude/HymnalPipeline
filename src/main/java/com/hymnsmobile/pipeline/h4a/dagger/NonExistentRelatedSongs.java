package com.hymnsmobile.pipeline.h4a.dagger;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier for list of songs that show up in "related" column but don't actually exist in the h4a db. These should be
 * ignored since they map to nothing.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface NonExistentRelatedSongs {
}

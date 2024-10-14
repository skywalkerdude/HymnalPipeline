package com.hymnsmobile.pipeline.dedup.dagger;

import javax.inject.Scope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Objects will last the lifetime of deduplication step of the pipeline.
 */
@Scope
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface DedupScope {
}

package com.hymnsmobile.pipeline.storage.dagger;

import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;

import java.util.HashSet;
import java.util.Set;

@Module
interface StorageModule {

  @Storage
  @Provides
  @StorageScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

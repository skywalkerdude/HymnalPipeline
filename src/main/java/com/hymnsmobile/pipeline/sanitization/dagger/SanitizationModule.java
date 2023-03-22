package com.hymnsmobile.pipeline.sanitization.dagger;

import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface SanitizationModule {

  @Sanitization
  @Provides
  @SanitizationScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

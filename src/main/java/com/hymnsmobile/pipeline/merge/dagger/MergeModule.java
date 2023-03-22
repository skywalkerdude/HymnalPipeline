package com.hymnsmobile.pipeline.merge.dagger;

import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface MergeModule {

  @Merge
  @Provides
  @MergeScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

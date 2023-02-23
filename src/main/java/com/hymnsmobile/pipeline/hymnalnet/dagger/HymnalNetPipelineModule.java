package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface HymnalNetPipelineModule {

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<Hymn> hymns() {
    return new HashSet<>();
  }

  @HymnalNet
  @Provides
  @HymnalNetPipelineScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

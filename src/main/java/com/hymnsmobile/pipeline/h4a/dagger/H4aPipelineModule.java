package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.h4a.models.H4aHymn;
import com.hymnsmobile.pipeline.models.PipelineError;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface H4aPipelineModule {

  @Provides
  @H4aPipelineScope
  static Set<H4aHymn> H4aHymns() {
    return new HashSet<>();
  }

  @H4a
  @Provides
  @H4aPipelineScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

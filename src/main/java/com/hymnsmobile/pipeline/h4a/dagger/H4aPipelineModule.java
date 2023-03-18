package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.models.Hymn;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface H4aPipelineModule {

  @H4a
  @Provides
  @H4aPipelineScope
  static Set<Hymn> hymns() {
    return new HashSet<>();
  }
}

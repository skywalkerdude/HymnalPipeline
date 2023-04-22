package com.hymnsmobile.pipeline.songbase.dagger;

import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.songbase.models.SongbaseHymn;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

@Module
interface SongbasePipelineModule {

  @Provides
  @SongbasePipelineScope
  static Set<SongbaseHymn> songbaseHymns() {
    return new HashSet<>();
  }

  @Songbase
  @Provides
  @SongbasePipelineScope
  static Set<PipelineError> errors() {
    return new HashSet<>();
  }
}

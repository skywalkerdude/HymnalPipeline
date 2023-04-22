package com.hymnsmobile.pipeline.songbase.dagger;

import com.hymnsmobile.pipeline.songbase.SongbasePipeline;
import dagger.Subcomponent;

@SongbasePipelineScope
@Subcomponent(modules = SongbasePipelineModule.class)
public interface SongbasePipelineComponent {

  SongbasePipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    SongbasePipelineComponent build();
  }
}

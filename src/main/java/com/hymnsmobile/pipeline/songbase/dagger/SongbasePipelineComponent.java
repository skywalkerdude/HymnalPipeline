package com.hymnsmobile.pipeline.songbase.dagger;

import com.hymnsmobile.pipeline.songbase.SongbaseDiffer;
import com.hymnsmobile.pipeline.songbase.SongbasePipeline;
import dagger.Subcomponent;

@SongbasePipelineScope
@Subcomponent(modules = SongbasePipelineModule.class)
public interface SongbasePipelineComponent {

  SongbasePipeline pipeline();

  SongbaseDiffer differ();

  @Subcomponent.Builder
  interface Builder {

    SongbasePipelineComponent build();
  }
}

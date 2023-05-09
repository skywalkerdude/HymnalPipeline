package com.hymnsmobile.pipeline.russian.dagger;

import com.hymnsmobile.pipeline.russian.RussianPipeline;
import dagger.Subcomponent;

@RussianScope
@Subcomponent(modules = RussianModule.class)
public interface RussianPipelineComponent {

  RussianPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    RussianPipelineComponent build();
  }
}

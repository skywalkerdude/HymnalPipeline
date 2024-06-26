package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.h4a.H4aPipeline;
import dagger.Subcomponent;

@H4aPipelineScope
@Subcomponent(modules = H4aPipelineModule.class)
public interface H4aPipelineComponent {

  H4aPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {
    H4aPipelineComponent build();
  }
}

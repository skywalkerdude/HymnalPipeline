package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import dagger.Subcomponent;

@HymnalNetPipelineScope
@Subcomponent(modules = HymnalNetPipelineModule.class)
public interface HymnalNetPipelineComponent {

  HymnalNetPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    HymnalNetPipelineComponent build();
  }
}

package com.hymnsmobile.pipeline.hymnalnet.dagger;

import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import dagger.Subcomponent;

@HymnalNetPipelineScope
@Subcomponent(modules = HymnalNetPipelineTestModule.class)
public interface HymnalNetPipelineTestComponent {

  HymnalNetPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    HymnalNetPipelineTestComponent build();
  }
}

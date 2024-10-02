package com.hymnsmobile.pipeline.h4a.dagger;

import com.hymnsmobile.pipeline.h4a.H4aPipeline;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import dagger.Subcomponent;

@H4aPipelineScope
@Subcomponent(modules = H4aPipelineTestModule.class)
public interface H4aPipelineTestComponent {

  H4aPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {
    H4aPipelineTestComponent build();
  }
}

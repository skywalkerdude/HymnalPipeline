package com.hymnsmobile.pipeline.liederbuch.dagger;

import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import com.hymnsmobile.pipeline.liederbuch.LiederbuchPipeline;
import dagger.Subcomponent;

@LiederbuchScope
@Subcomponent(modules = LiederbuchModule.class)
public interface LiederbuchPipelineComponent {

  LiederbuchPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    LiederbuchPipelineComponent build();
  }
}

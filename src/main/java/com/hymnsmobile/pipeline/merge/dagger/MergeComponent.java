package com.hymnsmobile.pipeline.merge.dagger;

import com.hymnsmobile.pipeline.merge.MergePipeline;
import dagger.Subcomponent;

@MergeScope
@Subcomponent(modules = MergeModule.class)
public interface MergeComponent {

  MergePipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    MergeComponent build();
  }
}

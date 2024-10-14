package com.hymnsmobile.pipeline.dedup.dagger;

import com.hymnsmobile.pipeline.dedup.DedupPipeline;
import dagger.Subcomponent;

@DedupScope
@Subcomponent(modules = DedupModule.class)
public interface DedupComponent {

  DedupPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    DedupComponent build();
  }
}

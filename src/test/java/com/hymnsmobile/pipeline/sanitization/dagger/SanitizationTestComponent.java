package com.hymnsmobile.pipeline.sanitization.dagger;

import com.hymnsmobile.pipeline.merge.SanitizationPipeline;
import dagger.Subcomponent;

@SanitizationScope
@Subcomponent(modules = SanitizationModule.class)
public interface SanitizationTestComponent {

  SanitizationPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    SanitizationTestComponent build();
  }
}
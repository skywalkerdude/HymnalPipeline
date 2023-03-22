package com.hymnsmobile.pipeline.sanitization.dagger;

import com.hymnsmobile.pipeline.sanitization.SanitizationPipeline;
import dagger.Subcomponent;

@SanitizationScope
@Subcomponent(modules = SanitizationModule.class)
public interface SanitizationComponent {

  SanitizationPipeline pipeline();

  @Subcomponent.Builder
  interface Builder {

    SanitizationComponent build();
  }
}

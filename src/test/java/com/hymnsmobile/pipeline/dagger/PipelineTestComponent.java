package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineTestComponent;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationTestComponent;
import dagger.Component;

@PipelineScope
@Component(modules = {PipelineTestModule.class})
public interface PipelineTestComponent extends PipelineComponent {

  HymnalNetPipelineTestComponent.Builder hymnalNetTestComponent();

  SanitizationTestComponent.Builder sanitizationTestComponent();
}

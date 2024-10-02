package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineTestComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineTestComponent;
import dagger.Component;

@PipelineScope
@Component(modules = {PipelineTestModule.class})
public interface PipelineTestComponent extends PipelineComponent {
  HymnalNetPipelineTestComponent.Builder hymnalNetTestComponent();
  H4aPipelineTestComponent.Builder h4aTestComponent();
}

package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.Pipeline;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import com.hymnsmobile.pipeline.merge.dagger.MergeComponent;
import com.hymnsmobile.pipeline.sanitization.dagger.SanitizationComponent;
import com.hymnsmobile.pipeline.storage.dagger.StorageComponent;
import dagger.Component;
import java.net.http.HttpClient;

@PipelineScope
@Component(modules = {PipelineModule.class})
public interface PipelineComponent {

  HttpClient httpClient();

  HymnalNetPipelineComponent.Builder hymnalNetComponent();
  H4aPipelineComponent.Builder h4aComponent();
  MergeComponent.Builder mergeComponent();
  SanitizationComponent.Builder sanitizationComponent();
  StorageComponent.Builder storageComponent();

  Pipeline pipeline();
}

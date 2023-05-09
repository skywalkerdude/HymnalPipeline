package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.Pipeline;
import com.hymnsmobile.pipeline.h4a.dagger.H4aPipelineComponent;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import com.hymnsmobile.pipeline.liederbuch.dagger.LiederbuchPipelineComponent;
import com.hymnsmobile.pipeline.merge.dagger.MergeComponent;
import com.hymnsmobile.pipeline.russian.dagger.RussianPipelineComponent;
import com.hymnsmobile.pipeline.songbase.dagger.SongbasePipelineComponent;
import com.hymnsmobile.pipeline.storage.dagger.StorageComponent;
import dagger.Component;
import java.net.http.HttpClient;

@PipelineScope
@Component(modules = {PipelineModule.class})
public interface PipelineComponent {

  HttpClient httpClient();

  HymnalNetPipelineComponent.Builder hymnalNetComponent();
  H4aPipelineComponent.Builder h4aComponent();

  LiederbuchPipelineComponent.Builder liederbuchPipelineComponent();
  MergeComponent.Builder mergeComponent();
  RussianPipelineComponent.Builder russianPipeline();
  SongbasePipelineComponent.Builder songbasePipelineComponent();
  StorageComponent.Builder storageComponent();

  Pipeline pipeline();
}

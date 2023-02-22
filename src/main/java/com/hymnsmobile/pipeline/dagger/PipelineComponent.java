package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.Pipeline;
import com.hymnsmobile.pipeline.hymnalnet.HymnalNetPipeline;
import dagger.Component;
import java.net.http.HttpClient;

@PipelineScope
@Component(modules = {PipelineModule.class})
public interface PipelineComponent {

  HttpClient httpClient();

  HymnalNetPipeline hymnalNetPipeline();

  void inject(Pipeline pipeline);
}

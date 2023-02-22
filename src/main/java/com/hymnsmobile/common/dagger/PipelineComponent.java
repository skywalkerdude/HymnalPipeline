package com.hymnsmobile.common.dagger;

import com.hymnsmobile.Pipeline;
import com.hymnsmobile.hymnalnet.HymnalNetPipeline;
import dagger.Component;
import java.net.http.HttpClient;

@PipelineScope
@Component(modules = {PipelineModule.class})
public interface PipelineComponent {

  HttpClient httpClient();

  HymnalNetPipeline hymnalNetPipeline();

  void inject(Pipeline pipeline);
}

package com.hymnsmobile.pipeline.dagger;

import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineComponent;
import dagger.Module;
import dagger.Provides;
import java.net.http.HttpClient;

@Module(subcomponents = HymnalNetPipelineComponent.class)
interface PipelineModule {

  @Provides
  static HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }
}
